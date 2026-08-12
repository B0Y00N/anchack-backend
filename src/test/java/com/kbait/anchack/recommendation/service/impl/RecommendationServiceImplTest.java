package com.kbait.anchack.recommendation.service.impl;

import com.kbait.anchack.admindong.domain.AdminDong;
import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RankedRecommendation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
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

        when(hardFilterService.filter(any())).thenReturn(List.of(candidate(1L)));
        when(recommendationReasonClient.generate(any())).thenReturn(placeholderReason());
        when(adminDongMapper.findById(any())).thenReturn(adminDong());
        mockInsertAssignsIds();
    }

    @Test
    void 상위_5개만_저장하고_반환한다() {
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(7));

        List<RecommendationRow> result = service.generate(condition());

        assertThat(result).hasSize(5);
        assertThat(result).extracting(RecommendationRow::getRank).containsExactly(1, 2, 3, 4, 5);
    }

    @Test
    void 삭제는_scores_먼저_recommendations_다음_INSERT_순서로_호출된다() {
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(1));

        service.generate(condition());

        InOrder inOrder = inOrder(recommendationScoreMapper, recommendationMapper);
        inOrder.verify(recommendationScoreMapper).deleteByConditionId(1L);
        inOrder.verify(recommendationMapper).deleteByConditionId(1L);
        inOrder.verify(recommendationMapper).insertBatch(anyList());
        inOrder.verify(recommendationScoreMapper).insertBatch(anyList());
    }

    @Test
    void INSERT_이후_반환값에_생성된_recommendationId가_채워진다() {
        when(recommendationScoreCalculator.calculate(any(), any())).thenReturn(rankedList(1));

        List<RecommendationRow> result = service.generate(condition());

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

    private AdminDong adminDong() {
        AdminDong adminDong = new AdminDong();
        adminDong.setGuName("마포구");
        adminDong.setName("서교동");

        return adminDong;
    }

    private GeneratedReason placeholderReason() {
        return GeneratedReason.builder()
                .recommendationReason("추후 openai api 호출")
                .caution("추후 openai api 호출")
                .build();
    }
}
