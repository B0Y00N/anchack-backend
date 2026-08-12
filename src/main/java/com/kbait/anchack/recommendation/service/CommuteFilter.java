package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongLocation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.RecommendationAdminDongMapper;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import com.kbait.anchack.route.exception.RouteNotFoundException;
import com.kbait.anchack.route.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 통근 하드필터. destAddress가 없으면 그대로 통과, 있으면 목적지를 한 번만
 * geocoding한 뒤 후보별 통근시간/환승횟수를 계산해 조건을 만족하는 후보만 남긴다.
 * 경로를 찾지 못한 후보(RouteNotFoundException)는 통근 조건을 만족하지 못한 것이므로
 * 조용히 제외한다. 반면 카카오 API 호출 자체가 실패한 후보(KakaoRouteApiException -
 * 네트워크/쿼터초과 등)는 조건 충족 여부를 알 수 없을 뿐 조건 불만족이 확인된 게
 * 아니므로 제외하지 않고 commuteTime/transferCount를 비운 채 결과에 포함시켜 소프트
 * 스코어링으로 넘긴다. 목적지 주소 자체를 geocoding하지 못하는 경우(AddressNotFoundException)는
 * 루프 진입 전 단계라 요청 전체가 그대로 실패한다. 계산된 값은 이후
 * recommendations.commute_time/transfer_count에 재사용하기 위해 결과에 함께 담아 반환한다.
 *
 * 후보별 route API 호출은 순차로 하면 후보가 많을 때(수백 건) 응답이 너무 느려져서
 * 고정 크기 스레드풀로 병렬 호출한다. 카카오 테스트 앱은 초당/일일 쿼터가 낮을 수
 * 있어 동시 호출 수를 적당히 제한(COMMUTE_CALL_CONCURRENCY)한다.
 *
 * TODO: 426개 후보를 동시 10개로 처리할 때 카카오 쪽에서 간헐적으로 429(TooManyRequests,
 * 순간 레이트리밋)가 발생하는 걸 확인함(2026-08-12 로그 기준 309건 실패 중 3건).
 * 고정 스레드풀이 한 호출이 끝나자마자 바로 다음 호출을 이어서 쏘는 구조라 지속적으로
 * 높은 처리량이 유지되는 게 원인으로 보임 - 나중에 여유 있을 때 동시 호출 수를 낮추거나
 * 호출 사이 딜레이/스로틀링을 두는 걸 검토. 지금 실패 대부분(306/309)은 이것과 무관한
 * 일일 쿼터 초과라 별개 이슈.
 *
 * 다른 구현체가 나올 여지가 없는 단일 필터 로직이라 인터페이스 분리 없이
 * 구현체만 둠.
 */
@Component
@RequiredArgsConstructor
public class CommuteFilter {

    private static final int COMMUTE_CALL_CONCURRENCY = 10;

    private final RecommendationAdminDongMapper adminDongMapper;
    private final RouteService routeService;

    public List<RecommendationCandidate> filter(
            List<Long> candidateAdminDongIds,
            String destAddress,
            String commuteType,
            Integer maxCommuteTime,
            Integer maxTransferCount
    ) {
        if (destAddress == null || destAddress.isBlank()) {
            return candidateAdminDongIds.stream()
                    .map(adminDongId -> RecommendationCandidate.builder().adminDongId(adminDongId).build())
                    .toList();
        }

        Coordinates destination = routeService.geocode(destAddress);
        List<AdminDongLocation> locations = adminDongMapper.findLocationsByIds(candidateAdminDongIds);

        if (locations.isEmpty()) {
            return List.of();
        }

        List<RecommendationCandidate> results = callCommuteApiInParallel(locations, destination, commuteType);

        return results.stream()
                .filter(candidate -> candidate != null && withinCommuteLimits(candidate, maxCommuteTime, maxTransferCount))
                .toList();
    }

    private List<RecommendationCandidate> callCommuteApiInParallel(
            List<AdminDongLocation> locations,
            Coordinates destination,
            String commuteType
    ) {
        int poolSize = Math.min(COMMUTE_CALL_CONCURRENCY, locations.size());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);

        try {
            List<CompletableFuture<RecommendationCandidate>> futures = locations.stream()
                    .map(location -> CompletableFuture.supplyAsync(
                            () -> tryCalculateCommute(location, destination, commuteType), executor))
                    .toList();

            return futures.stream()
                    .map(CompletableFuture::join)
                    .toList();
        } finally {
            executor.shutdown();
        }
    }

    private RecommendationCandidate tryCalculateCommute(
            AdminDongLocation location,
            Coordinates destination,
            String commuteType
    ) {
        try {
            CommuteResult commuteResult = routeService.calculateCommute(
                    location.getLatitude(), location.getLongitude(),
                    destination.getLatitude(), destination.getLongitude(),
                    commuteType);

            return RecommendationCandidate.builder()
                    .adminDongId(location.getAdminDongId())
                    .commuteTime(commuteResult.getCommuteTime())
                    .transferCount(commuteResult.getTransferCount())
                    .build();
        } catch (RouteNotFoundException e) {
            return null;
        } catch (KakaoRouteApiException e) {
            System.err.println("[CommuteFilter] 통근 계산 실패: adminDongId=" + location.getAdminDongId()
                    + ", lat=" + location.getLatitude() + ", lng=" + location.getLongitude()
                    + ", message=" + e.getMessage());
            return RecommendationCandidate.builder()
                    .adminDongId(location.getAdminDongId())
                    .build();
        }
    }

    private boolean withinCommuteLimits(
            RecommendationCandidate candidate,
            Integer maxCommuteTime,
            Integer maxTransferCount
    ) {
        if (candidate.getCommuteTime() == null || candidate.getTransferCount() == null) {
            return true;
        }

        boolean timeOk = maxCommuteTime == null || candidate.getCommuteTime() <= maxCommuteTime;
        boolean transferOk = maxTransferCount == null || candidate.getTransferCount() <= maxTransferCount;

        return timeOk && transferOk;
    }
}
