package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.RecommendationAdminDongMapper;
import com.kbait.anchack.recommendation.service.CommuteFilter;
import com.kbait.anchack.recommendation.service.EssentialInfraFilter;
import com.kbait.anchack.recommendation.service.HardFilterService;
import com.kbait.anchack.recommendation.service.HouseTypeBudgetFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 하드필터를 비용이 저렴한 순서(지역구 → 필수인프라 → 주거유형/예산 → 통근)로
 * 적용해 외부 API(통근) 호출 대상을 최소화한다.
 */
@Service
@RequiredArgsConstructor
public class HardFilterServiceImpl implements HardFilterService {

    private final RecommendationAdminDongMapper adminDongMapper;
    private final EssentialInfraFilter essentialInfraFilter;
    private final HouseTypeBudgetFilter houseTypeBudgetFilter;
    private final CommuteFilter commuteFilter;

    @Override
    public List<RecommendationCandidate> filter(ConditionBundle condition) {
        List<Long> candidates = resolveBaseCandidates(condition.getDestAddress(), condition.getGuCodes());
        candidates = essentialInfraFilter.filter(candidates, condition.getEssentialCategories());
        candidates = houseTypeBudgetFilter.filter(
                candidates,
                condition.getRentalType(),
                condition.getPreferredHouseTypes(),
                condition.getMaxDeposit(),
                condition.getMaxRent(),
                condition.getMinArea());

        return commuteFilter.filter(
                candidates,
                condition.getDestAddress(),
                condition.getCommuteType(),
                condition.getMaxCommuteTime(),
                condition.getMaxTransferCount());
    }

    /**
     * destAddress(통근 조건)와 guCodes(지역구 조건)는 상호 배타적으로 들어온다(프론트에서
     * 둘 중 하나만 채워 보냄). destAddress가 있으면 통근 필터가 전체 행정동을 대상으로
     * 좌표 기반 필터링을 하므로 guCodes는 무시하고 전체를 후보로 삼는다.
     */
    private List<Long> resolveBaseCandidates(String destAddress, List<String> guCodes) {
        if (destAddress != null && !destAddress.isBlank()) {
            return adminDongMapper.findAllIds();
        }

        return (guCodes == null || guCodes.isEmpty())
                ? adminDongMapper.findAllIds()
                : adminDongMapper.findIdsByGuCodes(guCodes);
    }
}
