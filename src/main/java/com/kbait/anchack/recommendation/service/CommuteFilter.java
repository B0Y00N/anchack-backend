package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongLocation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.RecommendationAdminDongMapper;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
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
 * 경로를 찾지 못한 후보(RouteNotFoundException)는 조용히 제외하고, 그 외
 * 예외(주소 자체를 못 찾음, 카카오 API 호출 실패 등)는 요청 전체를 실패시키기
 * 위해 그대로 전파한다. 계산된 값은 이후
 * recommendations.commute_time/transfer_count에 재사용하기 위해 결과에 함께 담아 반환한다.
 *
 * 후보별 route API 호출은 순차로 하면 후보가 많을 때(수백 건) 응답이 너무 느려져서
 * 고정 크기 스레드풀로 병렬 호출한다. 카카오 테스트 앱은 초당/일일 쿼터가 낮을 수
 * 있어 동시 호출 수를 적당히 제한(COMMUTE_CALL_CONCURRENCY)한다.
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
        }
    }

    private boolean withinCommuteLimits(
            RecommendationCandidate candidate,
            Integer maxCommuteTime,
            Integer maxTransferCount
    ) {
        boolean timeOk = maxCommuteTime == null || candidate.getCommuteTime() <= maxCommuteTime;
        boolean transferOk = maxTransferCount == null || candidate.getTransferCount() <= maxTransferCount;

        return timeOk && transferOk;
    }
}
