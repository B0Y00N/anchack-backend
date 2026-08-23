package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.FilterFunnelStage;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.HardFilterResult;
import com.kbait.anchack.recommendation.dto.RankedRecommendation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.dto.RecommendationComputation;
import com.kbait.anchack.recommendation.dto.RecommendationRow;
import com.kbait.anchack.recommendation.mapper.RecommendationMapper;
import com.kbait.anchack.recommendation.mapper.RecommendationScoreMapper;
import com.kbait.anchack.recommendation.service.HardFilterService;
import com.kbait.anchack.recommendation.service.RecommendationScoreCalculator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @Mock
    private HardFilterService hardFilterService;

    @Mock
    private RecommendationScoreCalculator recommendationScoreCalculator;

    @Mock
    private RecommendationReasonClient recommendationReasonClient;

    @Mock
    private RecommendationMapper recommendationMapper;

    @Mock
    private RecommendationScoreMapper recommendationScoreMapper;

    @Mock
    private AdminDongMapper adminDongMapper;

    private RecommendationServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RecommendationServiceImpl(
                hardFilterService,
                recommendationScoreCalculator,
                recommendationReasonClient,
                recommendationMapper,
                recommendationScoreMapper,
                adminDongMapper);

        when(hardFilterService.filter(any())).thenReturn(hardFilterResult(List.of(candidate(1L))));
        // 후보가 비어있는 테스트(하드필터_후보가_비어있으면...)에서는 topRanked가 비어있어
        // 아래 두 스텁이 안 쓰이므로 lenient로 둔다.
        lenient().when(recommendationReasonClient.generate(any())).thenReturn(placeholderReason());
        lenient().when(adminDongMapper.findById(any())).thenReturn(adminDong());
    }

    @Test
    void 상위_5개만_저장하고_반환한다() {
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(7));

        List<RecommendationRow> result = service.compute(condition()).getRows();

        assertThat(result).hasSize(5);
        assertThat(result).extracting(RecommendationRow::getRank).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void admin_dong의_구이름_동이름_좌표를_결과에_채운다() {
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(1));

        List<RecommendationRow> result = service.compute(condition()).getRows();

        assertThat(result.get(0).getGuName()).isEqualTo("마포구");
        assertThat(result.get(0).getDongName()).isEqualTo("서교동");
        assertThat(result.get(0).getLatitude()).isEqualByComparingTo("37.5");
        assertThat(result.get(0).getLongitude()).isEqualByComparingTo("127.0");
    }

    @Test
    void 하드필터_퍼널에_FINAL_단계를_이어붙여_반환한다() {
        when(hardFilterService.filter(any())).thenReturn(hardFilterResult(
                List.of(candidate(1L), candidate(2L), candidate(3L)),
                stage("TOTAL", 426), stage("BUDGET", 3)));
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(3));

        List<FilterFunnelStage> funnel = service.compute(condition()).getFilterFunnel();

        assertThat(funnel)
                .extracting(FilterFunnelStage::getStage, FilterFunnelStage::getCount)
                .containsExactly(tuple("TOTAL", 426), tuple("BUDGET", 3), tuple("FINAL", 3));
    }

    @Test
    void 하드필터_후보가_비어있으면_FINAL_단계를_이어붙이지_않는다() {
        when(hardFilterService.filter(any())).thenReturn(hardFilterResult(List.of(), stage("TOTAL", 0)));
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(List.of());

        List<FilterFunnelStage> funnel = service.compute(condition()).getFilterFunnel();

        assertThat(funnel)
                .extracting(FilterFunnelStage::getStage)
                .containsExactly("TOTAL");
    }

    @Test
    void 삭제는_scores_먼저_recommendations_다음_INSERT_순서로_호출된다() {
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(1));
        mockInsertAssignsIds();
        List<RecommendationRow> computed = service.compute(condition()).getRows();

        service.persist(1L, computed);

        InOrder inOrder = inOrder(recommendationScoreMapper, recommendationMapper);
        inOrder.verify(recommendationScoreMapper).deleteByConditionId(1L);
        inOrder.verify(recommendationMapper).deleteByConditionId(1L);
        inOrder.verify(recommendationMapper).insertBatch(anyList());
        inOrder.verify(recommendationScoreMapper).insertBatch(anyList());
    }

    @Test
    void INSERT_이후_반환값에_생성된_recommendationId가_채워진다() {
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(1));
        mockInsertAssignsIds();
        List<RecommendationRow> computed = service.compute(condition()).getRows();

        List<RecommendationRow> result = service.persist(1L, computed);

        assertThat(result.get(0).getRecommendationId()).isEqualTo(100L);
    }

    private void mockInsertAssignsIds() {
        doAnswer(invocation -> {
            List<RecommendationRow> rows = invocation.getArgument(0);
            long id = 100L;
            for (RecommendationRow row : rows) {
                row.setRecommendationId(id++);
            }
            return rows.size();
        }).when(recommendationMapper).insertBatch(anyList());
    }

    private List<RankedRecommendation> rankedList(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> RankedRecommendation.builder()
                        .adminDongId((long) i)
                        .totalScore(BigDecimal.valueOf(100 - i))
                        .dataCoverageRate(BigDecimal.valueOf(100))
                        .rank(i)
                        .categoryBreakdowns(List.of(breakdown()))
                        .build())
                .toList();
    }

    private CategoryScoreBreakdown breakdown() {
        return CategoryScoreBreakdown.builder()
                .category("SAFETY")
                .rawScore(new BigDecimal("70.00"))
                .weight(new BigDecimal("1.0"))
                .weightedScore(new BigDecimal("0.50"))
                .build();
    }

    private ConditionBundle condition() {
        return ConditionBundle.builder().conditionId(1L).build();
    }

    private RecommendationCandidate candidate(Long adminDongId) {
        return RecommendationCandidate.builder().adminDongId(adminDongId).build();
    }

    private HardFilterResult hardFilterResult(List<RecommendationCandidate> candidates, FilterFunnelStage... stages) {
        return HardFilterResult.builder().candidates(candidates).filterFunnel(List.of(stages)).build();
    }

    private FilterFunnelStage stage(String stageCode, int count) {
        return FilterFunnelStage.builder().stage(stageCode).label(stageCode).count(count).build();
    }

    private AdminDong adminDong() {
        AdminDong adminDong = new AdminDong();
        adminDong.setGuName("마포구");
        adminDong.setName("서교동");
        adminDong.setLatitude(new BigDecimal("37.5"));
        adminDong.setLongitude(new BigDecimal("127.0"));

        return adminDong;
    }

    private GeneratedReason placeholderReason() {
        return GeneratedReason.builder()
                .recommendationReason("추후 openai api 호출")
                .caution("추후 openai api 호출")
                .build();
    }
}
