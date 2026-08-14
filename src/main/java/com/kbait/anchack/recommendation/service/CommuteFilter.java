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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
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
 * 스코어링으로 넘긴다. 목적지 자체를 geocoding하는 단계에서도 같은 원칙을 적용한다:
 * 주소를 아예 못 찾은 경우(AddressNotFoundException, 사용자 입력 오류)는 요청 전체를 그대로
 * 실패시키지만, 카카오 API 호출 자체가 실패한 경우(KakaoRouteApiException)는 후보 전체를
 * 통근 정보 없이 통과시킨다(destAddress를 안 보낸 것과 동일하게 처리). 계산된 값은 이후
 * recommendations.commute_time/transfer_count에 재사용하기 위해 결과에 함께 담아 반환한다.
 *
 * 후보별 route API 호출은 순차로 하면 후보가 많을 때(수백 건) 응답이 너무 느려져서
 * 고정 크기 스레드풀로 병렬 호출한다. 카카오 테스트 앱은 초당/일일 쿼터가 낮을 수
 * 있어 동시 호출 수를 적당히 제한(COMMUTE_CALL_CONCURRENCY)한다.
 *
 * 426개 후보를 동시 10개로 처리할 때 카카오 쪽에서 간헐적으로 429(TooManyRequests, 순간
 * 레이트리밋)와 400(API limit has been exceeded)이 발생하는 걸 확인했다. 초기에는
 * 이 실패들을 일일 쿼터 소진으로 추정했으나, 이후 요청 순번/시각을 계측해 재분석한
 * 결과 벽시계 기준 약 1초 단위로 성공/실패 구간이 반복되는 패턴이 관찰돼 초당(단기
 * window) 호출 제한(현재 환경 실측 기준 약 20 req/s 부근 - 카카오가 공식 수치를
 * 공개하지 않아 확정치는 아님)이 주된 원인이라고 판단을 수정했다. 일일 쿼터는 이것과
 * 별개로 존재하는 것으로 보인다(자세한 경위는 docs/ROUTE_API_RATE_LIMIT_ISSUE.md 참고).
 * 429는 route.client의 KakaoGeocodingClient/KakaoTransitDirectionsClient가 짧게 대기
 * 후 1회 재시도하는 것으로 대응한다 - 재시도로도 안 되면 여기(tryCalculateCommute)까지
 * KakaoRouteApiException으로 올라와 해당 후보만 통근 정보 없이 소프트 처리된다.
 *
 * 다른 구현체가 나올 여지가 없는 단일 필터 로직이라 인터페이스 분리 없이
 * 구현체만 둠.
 *
 * 실측 결과 대중교통 경로 API는 현재 환경에서 약 20 req/s 부근의 단기 호출 제한이 관찰되었으므로,
 * 안전 마진을 두어 애플리케이션 전체 호출을 약 12.5 req/s로 pacing하고(KakaoTransitDirectionsClient
 * 참고), 동시에 추천 요청 1건의 최대 외부 API fan-out을 Haversine 기반 MAX_COMMUTE_ROUTE_CANDIDATES건으로
 * 제한한다. 후보 수가 이를 초과하면 목적지와의 직선거리가 가까운 순으로 잘라내고, 이 직선거리는
 * 실제 통근시간 판단에는 쓰지 않는다 - 순수하게 호출 대상을 좁히는 사전 필터링 기준일 뿐이다.
 */
@Component
@RequiredArgsConstructor
public class CommuteFilter {

    private static final Logger log = LoggerFactory.getLogger(CommuteFilter.class);

    private static final int COMMUTE_CALL_CONCURRENCY = 10;
    private static final int MAX_COMMUTE_ROUTE_CANDIDATES = 50;
    private static final double EARTH_RADIUS_METERS = 6_371_000;

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
            return passThroughWithoutCommuteInfo(candidateAdminDongIds);
        }

        Coordinates destination;
        try {
            destination = routeService.geocode(destAddress);
        } catch (KakaoRouteApiException e) {
            // 목적지 좌표를 못 구하면 후보별 통근 계산 자체가 불가능하다. AddressNotFoundException(사용자
            // 입력 오류)과 달리 이건 카카오 쪽 장애이므로 요청을 실패시키지 않고 통근 정보 없이 진행한다.
            log.warn("목적지 geocoding 실패, 통근 정보 없이 진행: destAddress={}, message={}",
                    destAddress, e.getMessage());
            return passThroughWithoutCommuteInfo(candidateAdminDongIds);
        }

        List<AdminDongLocation> locations = adminDongMapper.findLocationsByIds(candidateAdminDongIds);

        if (locations.isEmpty()) {
            return List.of();
        }

        if (locations.size() > MAX_COMMUTE_ROUTE_CANDIDATES) {
            locations = locations.stream()
                    .sorted(Comparator.comparingDouble(location -> haversineDistanceMeters(location, destination)))
                    .limit(MAX_COMMUTE_ROUTE_CANDIDATES)
                    .toList();
        }

        List<RecommendationCandidate> results = callCommuteApiInParallel(locations, destination, commuteType);

        return results.stream()
                .filter(candidate -> candidate != null && withinCommuteLimits(candidate, maxCommuteTime, maxTransferCount))
                .toList();
    }

    private double haversineDistanceMeters(AdminDongLocation location, Coordinates destination) {
        double lat1 = Math.toRadians(location.getLatitude().doubleValue());
        double lat2 = Math.toRadians(destination.getLatitude().doubleValue());
        double deltaLat = lat2 - lat1;
        double deltaLng = Math.toRadians(
                destination.getLongitude().doubleValue() - location.getLongitude().doubleValue());

        double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(deltaLng / 2) * Math.sin(deltaLng / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));

        return EARTH_RADIUS_METERS * c;
    }

    private List<RecommendationCandidate> passThroughWithoutCommuteInfo(List<Long> candidateAdminDongIds) {
        return candidateAdminDongIds.stream()
                .map(adminDongId -> RecommendationCandidate.builder().adminDongId(adminDongId).build())
                .toList();
    }

    /**
     * 실패 패턴 분석을 위한 임시 계측. requestSeq는 이 배치 내 제출 순서(=locations 리스트
     * 순서)를 나타내고, batchId/requestSeq를 MDC에 실어 같은 워커 스레드에서 동기 호출되는
     * KakaoTransitDirectionsClient의 429 재시도 로그에도 자동으로 correlation 정보가
     * 찍히게 한다(클라이언트 쪽 시그니처는 건드리지 않음). 원인 분석이 끝나면 이 로깅과
     * logback.xml의 %X{...} 패턴은 제거해도 된다.
     */
    private List<RecommendationCandidate> callCommuteApiInParallel(
            List<AdminDongLocation> locations,
            Coordinates destination,
            String commuteType
    ) {
        int poolSize = Math.min(COMMUTE_CALL_CONCURRENCY, locations.size());
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        String batchId = UUID.randomUUID().toString().substring(0, 8);

        log.info("통근 API 호출 배치 시작: batchId={}, candidateCount={}, concurrency={}",
                batchId, locations.size(), poolSize);

        try {
            List<CompletableFuture<RecommendationCandidate>> futures = new ArrayList<>();
            for (int i = 0; i < locations.size(); i++) {
                AdminDongLocation location = locations.get(i);
                int requestSeq = i + 1;
                futures.add(CompletableFuture.supplyAsync(
                        () -> tryCalculateCommute(location, destination, commuteType, batchId, requestSeq),
                        executor));
            }

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
            String commuteType,
            String batchId,
            int requestSeq
    ) {
        MDC.put("commuteBatchId", batchId);
        MDC.put("commuteSeq", String.valueOf(requestSeq));
        Instant startedAt = Instant.now();

        try {
            CommuteResult commuteResult = routeService.calculateCommute(
                    location.getLatitude(), location.getLongitude(),
                    destination.getLatitude(), destination.getLongitude(),
                    commuteType);
            Instant endedAt = Instant.now();

            log.debug("통근 계산 성공: adminDongId={}, startedAt={}, endedAt={}, durationMs={}, commuteTime={}, transferCount={}",
                    location.getAdminDongId(), startedAt, endedAt, Duration.between(startedAt, endedAt).toMillis(),
                    commuteResult.getCommuteTime(), commuteResult.getTransferCount());

            return RecommendationCandidate.builder()
                    .adminDongId(location.getAdminDongId())
                    .commuteTime(commuteResult.getCommuteTime())
                    .transferCount(commuteResult.getTransferCount())
                    .build();
        } catch (RouteNotFoundException e) {
            Instant endedAt = Instant.now();
            log.debug("통근 계산 결과 경로 없음: adminDongId={}, startedAt={}, endedAt={}, durationMs={}",
                    location.getAdminDongId(), startedAt, endedAt, Duration.between(startedAt, endedAt).toMillis());
            return null;
        } catch (KakaoRouteApiException e) {
            Instant endedAt = Instant.now();
            log.warn("통근 계산 실패, 통근 정보 없이 포함: adminDongId={}, startedAt={}, endedAt={}, durationMs={}, lat={}, lng={}, message={}",
                    location.getAdminDongId(), startedAt, endedAt, Duration.between(startedAt, endedAt).toMillis(),
                    location.getLatitude(), location.getLongitude(), e.getMessage());
            return RecommendationCandidate.builder()
                    .adminDongId(location.getAdminDongId())
                    .build();
        } finally {
            MDC.remove("commuteBatchId");
            MDC.remove("commuteSeq");
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
