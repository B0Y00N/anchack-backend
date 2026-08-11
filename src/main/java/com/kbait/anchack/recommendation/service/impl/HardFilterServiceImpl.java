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
        List<Long> candidates = resolveBaseCandidates(condition.getGuCodes());
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

    private List<Long> resolveBaseCandidates(List<String> guCodes) {
        return (guCodes == null || guCodes.isEmpty())
                ? adminDongMapper.findAllIds()
                : adminDongMapper.findIdsByGuCodes(guCodes);
    }
}
