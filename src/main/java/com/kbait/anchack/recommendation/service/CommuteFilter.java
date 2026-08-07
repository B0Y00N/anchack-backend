package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongLocation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.AdminDongMapper;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 통근 하드필터. destAddress가 없으면 그대로 통과, 있으면 route 도메인을 통해 후보별
 * 통근시간/환승횟수를 계산해 조건을 만족하는 후보만 남긴다. 계산된 값은 이후
 * recommendations.commute_time/transfer_count에 재사용하기 위해 결과에 함께 담아 반환한다.
 *
 * TODO: 후보 수가 많아지면 candidate마다 순차적으로 route API를 호출하는 부분이 병목이
 * 될 수 있음 - route 공급자 확정 후 병렬 호출/배치 호출 지원 여부를 확인해서 개선.
 *
 * 다른 구현체가 나올 여지가 없는 단일 필터 로직이라 인터페이스 분리 없이
 * 구현체만 둠.
 */
@Component
@RequiredArgsConstructor
public class CommuteFilter {

    private final AdminDongMapper adminDongMapper;
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

        List<AdminDongLocation> locations = adminDongMapper.findLocationsByIds(candidateAdminDongIds);

        return locations.stream()
                .map(location -> toCandidate(location, destAddress, commuteType))
                .filter(candidate -> withinCommuteLimits(candidate, maxCommuteTime, maxTransferCount))
                .toList();
    }

    private RecommendationCandidate toCandidate(AdminDongLocation location, String destAddress, String commuteType) {
        CommuteResult commuteResult = routeService.calculateCommute(
                location.getLatitude(), location.getLongitude(), destAddress, commuteType);

        return RecommendationCandidate.builder()
                .adminDongId(location.getAdminDongId())
                .commuteTime(commuteResult.getCommuteTime())
                .transferCount(commuteResult.getTransferCount())
                .build();
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
