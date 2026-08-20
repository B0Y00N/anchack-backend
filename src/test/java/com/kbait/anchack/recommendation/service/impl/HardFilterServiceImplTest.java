package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 각 하드필터 단계에서 후보가 0개가 되면 다음 단계를 아예 호출하지 않는지 검증한다.
 * 빈 리스트를 다음 단계(매퍼의 admin_dong_id IN (...))로 그대로 넘기면 MyBatis
 * <foreach>가 빈 괄호를 렌더링해 SQL 문법 오류가 나기 때문에, 후보가 비었을 때
 * 실제로 매퍼가 호출되지 않는 것까지 확인해야 의미가 있다.
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
    void 기본_후보가_비어있으면_필수인프라_필터부터_호출하지_않는다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of());

        List<RecommendationCandidate> result = service.filter(bundle(List.of("11120"), List.of("편의점")));

        assertThat(result).isEmpty();
        verifyNoInteractions(essentialInfraFilter, houseTypeBudgetFilter, commuteFilter);
    }

    @Test
    void 필수인프라_필터_후_후보가_비면_예산_필터부터_호출하지_않는다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of(1L, 2L));
        when(essentialInfraFilter.filter(anyList(), any())).thenReturn(List.of());

        List<RecommendationCandidate> result = service.filter(bundle(List.of("11120"), List.of("편의점")));

        assertThat(result).isEmpty();
        verifyNoInteractions(houseTypeBudgetFilter, commuteFilter);
    }

    @Test
    void 예산_필터_후_후보가_비면_통근_필터를_호출하지_않는다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of(1L, 2L));
        when(essentialInfraFilter.filter(anyList(), any())).thenReturn(List.of(1L, 2L));
        when(houseTypeBudgetFilter.filter(anyList(), any(), any(), any(), any(), any())).thenReturn(List.of());

        List<RecommendationCandidate> result = service.filter(bundle(List.of("11120"), List.of("편의점")));

        assertThat(result).isEmpty();
        verifyNoInteractions(commuteFilter);
    }

    @Test
    void 모든_단계를_통과하면_통근_필터_결과를_그대로_반환한다() {
        when(adminDongMapper.findIdsByGuCodes(anyList())).thenReturn(List.of(1L, 2L));
        when(essentialInfraFilter.filter(anyList(), any())).thenReturn(List.of(1L, 2L));
        when(houseTypeBudgetFilter.filter(anyList(), any(), any(), any(), any(), any())).thenReturn(List.of(1L, 2L));
        List<RecommendationCandidate> expected = List.of(RecommendationCandidate.builder().adminDongId(1L).build());
        when(commuteFilter.filter(anyList(), any(), any(), any(), any())).thenReturn(expected);

        List<RecommendationCandidate> result = service.filter(bundle(List.of("11120"), List.of("편의점")));

        assertThat(result).isEqualTo(expected);
    }

    private ConditionBundle bundle(List<String> guCodes, List<String> essentialCategories) {
        return ConditionBundle.builder()
                .conditionId(1L)
                .rentalType("월세")
                .guCodes(guCodes)
                .essentialCategories(essentialCategories)
                .maxDeposit(5000L)
                .maxRent(100)
                .build();
    }
}
