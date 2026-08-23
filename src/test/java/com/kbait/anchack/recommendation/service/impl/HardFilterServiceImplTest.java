package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.FilterFunnelStage;
import com.kbait.anchack.recommendation.dto.HardFilterResult;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.RecommendationAdminDongMapper;
import com.kbait.anchack.recommendation.service.CommuteFilter;
import com.kbait.anchack.recommendation.service.EssentialInfraFilter;
import com.kbait.anchack.recommendation.service.HouseTypeBudgetFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 각 하드필터 단계에서 후보가 0개가 되면 다음 단계를 아예 호출하지 않는지, 그리고 각 단계를
 * 통과하고 남은 후보 수(filterFunnel, P3)가 정확히 기록되는지 검증한다. 빈 리스트를 다음
 * 단계(매퍼의 admin_dong_id IN (...))로 그대로 넘기면 MyBatis <foreach>가 빈 괄호를 렌더링해
 * SQL 문법 오류가 나기 때문에, 후보가 비었을 때 실제로 매퍼가 호출되지 않는 것까지 확인해야
 * 의미가 있다.
 */
@ExtendWith(MockitoExtension.class)
class HardFilterServiceImplTest {

    @Mock
    private RecommendationAdminDongMapper adminDongMapper;

    @Mock
    private EssentialInfraFilter essentialInfraFilter;

    @Mock
    private HouseTypeBudgetFilter houseTypeBudgetFilter;

    @Mock
    private CommuteFilter commuteFilter;

    private HardFilterServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new HardFilterServiceImpl(adminDongMapper, essentialInfraFilter, houseTypeBudgetFilter, commuteFilter);
    }

    @Test
    void 기본_후보가_비어있으면_필수인프라_필터부터_호출하지_않고_TOTAL_단계만_담긴다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of());

        HardFilterResult result = service.filter(bundle(List.of("11120"), List.of("편의점"), null));

        assertThat(result.getCandidates()).isEmpty();
        assertThat(result.getFilterFunnel())
                .extracting(FilterFunnelStage::getStage, FilterFunnelStage::getCount)
                .containsExactly(tuple("TOTAL", 0));
        verifyNoInteractions(essentialInfraFilter, houseTypeBudgetFilter, commuteFilter);
    }

    @Test
    void 필수인프라_필터_후_후보가_비면_예산_필터부터_호출하지_않고_ESSENTIAL까지만_담긴다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of(1L, 2L));
        when(essentialInfraFilter.filter(anyList(), any())).thenReturn(List.of());

        HardFilterResult result = service.filter(bundle(List.of("11120"), List.of("편의점"), null));

        assertThat(result.getCandidates()).isEmpty();
        assertThat(result.getFilterFunnel())
                .extracting(FilterFunnelStage::getStage, FilterFunnelStage::getCount)
                .containsExactly(tuple("TOTAL", 2), tuple("ESSENTIAL", 0));
        verifyNoInteractions(houseTypeBudgetFilter, commuteFilter);
    }

    @Test
    void 예산_필터_후_후보가_비면_통근_필터를_호출하지_않고_BUDGET까지만_담긴다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of(1L, 2L));
        when(essentialInfraFilter.filter(anyList(), any())).thenReturn(List.of(1L, 2L));
        when(houseTypeBudgetFilter.filter(anyList(), any(), any(), any(), any(), any())).thenReturn(List.of());

        HardFilterResult result = service.filter(bundle(List.of("11120"), List.of("편의점"), null));

        assertThat(result.getCandidates()).isEmpty();
        assertThat(result.getFilterFunnel())
                .extracting(FilterFunnelStage::getStage, FilterFunnelStage::getCount)
                .containsExactly(tuple("TOTAL", 2), tuple("ESSENTIAL", 2), tuple("BUDGET", 0));
        verifyNoInteractions(commuteFilter);
    }

    @Test
    void destAddress가_있으면_모든_단계를_통과해_통근_필터_결과와_COMMUTE_단계까지_담는다() {
        when(adminDongMapper.findAllIds()).thenReturn(List.of(1L, 2L));
        when(essentialInfraFilter.filter(anyList(), any())).thenReturn(List.of(1L, 2L));
        when(houseTypeBudgetFilter.filter(anyList(), any(), any(), any(), any(), any())).thenReturn(List.of(1L, 2L));
        List<RecommendationCandidate> expected = List.of(RecommendationCandidate.builder().adminDongId(1L).build());
        when(commuteFilter.filter(anyList(), any(), any(), any(), any())).thenReturn(expected);

        HardFilterResult result = service.filter(bundle(null, List.of("편의점"), "서울 강남구"));

        assertThat(result.getCandidates()).isEqualTo(expected);
        assertThat(result.getFilterFunnel())
                .extracting(FilterFunnelStage::getStage, FilterFunnelStage::getCount)
                .containsExactly(
                        tuple("TOTAL", 2), tuple("ESSENTIAL", 2), tuple("BUDGET", 2), tuple("COMMUTE", 1));
    }

    @Test
    void destAddress가_없으면_통근_필터는_호출되지만_COMMUTE_단계는_담기지_않는다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of(1L, 2L));
        when(essentialInfraFilter.filter(anyList(), any())).thenReturn(List.of(1L, 2L));
        when(houseTypeBudgetFilter.filter(anyList(), any(), any(), any(), any(), any())).thenReturn(List.of(1L, 2L));
        List<RecommendationCandidate> passthrough = List.of(
                RecommendationCandidate.builder().adminDongId(1L).build(),
                RecommendationCandidate.builder().adminDongId(2L).build());
        when(commuteFilter.filter(anyList(), any(), any(), any(), any())).thenReturn(passthrough);

        HardFilterResult result = service.filter(bundle(List.of("11120"), List.of("편의점"), null));

        assertThat(result.getCandidates()).isEqualTo(passthrough);
        assertThat(result.getFilterFunnel())
                .extracting(FilterFunnelStage::getStage)
                .containsExactly("TOTAL", "ESSENTIAL", "BUDGET");
    }

    private ConditionBundle bundle(List<String> guCodes, List<String> essentialCategories, String destAddress) {
        return ConditionBundle.builder()
                .conditionId(1L)
                .rentalType("월세")
                .guCodes(guCodes)
                .essentialCategories(essentialCategories)
                .destAddress(destAddress)
                .maxDeposit(5000L)
                .maxRent(100)
                .build();
    }
}
