package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.FilterFunnelStage;
import com.kbait.anchack.recommendation.dto.HardFilterResult;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.RecommendationAdminDongMapper;
import com.kbait.anchack.recommendation.service.CommuteFilter;
import com.kbait.anchack.recommendation.service.EssentialInfraFilter;
import com.kbait.anchack.recommendation.service.HardFilterService;
import com.kbait.anchack.recommendation.service.HouseTypeBudgetFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * 하드필터를 비용이 저렴한 순서(지역구 → 필수인프라 → 주거유형/예산 → 통근)로
 * 적용해 외부 API(통근) 호출 대상을 최소화한다.
 *
 * 각 단계 후보가 0개로 줄어들면 즉시 빈 결과를 반환하고 다음 단계를 아예 호출하지
 * 않는다 - EssentialInfraFilter/HouseTypeBudgetFilter/CommuteFilter가 내부에서 쓰는
 * MyBatis 매퍼는 candidateAdminDongIds를 admin_dong_id IN (...)로 그대로 바인딩하는데,
 * 빈 리스트를 넘기면 <foreach>가 빈 괄호 "IN ( )"를 그대로 렌더링해 SQL 문법 오류가 난다.
 *
 * 각 단계를 통과하고 남은 후보 수를 filterFunnel로 같이 반환한다(프론트 로딩 화면 퍼널
 * 표시용, P3). 어떤 단계에서 후보가 0개가 되면 그 시점까지의 단계만 담고 이후 단계는
 * 아예 실행되지 않으므로 배열에 포함하지 않는다 - "실행되지 않은 단계"와 "실행됐지만
 * 0개인 단계"를 구분하기 위함이다. destAddress가 없으면 통근 필터는 실행은 되지만
 * (그대로 통과시키기만 함) 실질적인 필터링이 아니므로 COMMUTE 단계 자체를 넣지 않는다.
 */
@Service
@RequiredArgsConstructor
public class HardFilterServiceImpl implements HardFilterService {

    private static final String STAGE_TOTAL = "TOTAL";
    private static final String STAGE_ESSENTIAL = "ESSENTIAL";
    private static final String STAGE_BUDGET = "BUDGET";
    private static final String STAGE_COMMUTE = "COMMUTE";

    private static final String LABEL_TOTAL = "검색 대상";
    private static final String LABEL_ESSENTIAL = "필수 시설 조건 충족";
    private static final String LABEL_BUDGET = "예산 조건 충족";
    private static final String LABEL_COMMUTE = "출퇴근 가능";

    private final RecommendationAdminDongMapper adminDongMapper;
    private final EssentialInfraFilter essentialInfraFilter;
    private final HouseTypeBudgetFilter houseTypeBudgetFilter;
    private final CommuteFilter commuteFilter;

    @Override
    public HardFilterResult filter(ConditionBundle condition) {
        List<FilterFunnelStage> funnel = new ArrayList<>();

        List<Long> candidates = resolveBaseCandidates(condition.getDestAddress(), condition.getGuCodes());
        funnel.add(stage(STAGE_TOTAL, LABEL_TOTAL, candidates.size()));

        if (candidates.isEmpty()) {
            return result(List.of(), funnel);
        }

        candidates = essentialInfraFilter.filter(candidates, condition.getEssentialCategories());
        funnel.add(stage(STAGE_ESSENTIAL, LABEL_ESSENTIAL, candidates.size()));

        if (candidates.isEmpty()) {
            return result(List.of(), funnel);
        }

        candidates = houseTypeBudgetFilter.filter(
                candidates,
                condition.getRentalType(),
                condition.getPreferredHouseTypes(),
                condition.getMaxDeposit(),
                condition.getMaxRent(),
                condition.getMinArea());
        funnel.add(stage(STAGE_BUDGET, LABEL_BUDGET, candidates.size()));

        if (candidates.isEmpty()) {
            return result(List.of(), funnel);
        }

        List<RecommendationCandidate> commuteResult = commuteFilter.filter(
                candidates,
                condition.getDestAddress(),
                condition.getCommuteType(),
                condition.getMaxCommuteTime(),
                condition.getMaxTransferCount());

        if (hasDestAddress(condition.getDestAddress())) {
            funnel.add(stage(STAGE_COMMUTE, LABEL_COMMUTE, commuteResult.size()));
        }

        return result(commuteResult, funnel);
    }

    private boolean hasDestAddress(String destAddress) {
        return destAddress != null && !destAddress.isBlank();
    }

    private FilterFunnelStage stage(String stageCode, String label, int count) {
        return FilterFunnelStage.builder().stage(stageCode).label(label).count(count).build();
    }

    private HardFilterResult result(List<RecommendationCandidate> candidates, List<FilterFunnelStage> funnel) {
        return HardFilterResult.builder().candidates(candidates).filterFunnel(funnel).build();
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
