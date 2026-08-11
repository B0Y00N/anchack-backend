package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongMetricScores;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.RankedRecommendation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.MetricScoreMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.groups.Tuple.tuple;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecommendationScoreCalculatorTest {

    @Mock
    private MetricScoreMapper metricScoreMapper;

    private RecommendationScoreCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new RecommendationScoreCalculator(metricScoreMapper);
    }

    @Test
    void 가중치는_선택된_카테고리_합으로_정규화된다() {
        when(metricScoreMapper.findAllLatestScores()).thenReturn(List.of(
                dong(1L, "80", "80"),
                dong(2L, "50", "50"),
                dong(3L, "20", "20")
        ));
        ConditionBundle condition = conditionWithWeights(Map.of("SAFETY", "3", "CULTURE", "1"));

        List<RankedRecommendation> result = calculator.calculate(List.of(candidate(1L)), condition);

        Map<String, CategoryScoreBreakdown> breakdownByCategory = breakdownByCategory(result.get(0));
        assertThat(breakdownByCategory.get("SAFETY").getWeight()).isEqualByComparingTo("0.75");
        assertThat(breakdownByCategory.get("CULTURE").getWeight()).isEqualByComparingTo("0.25");
    }

    @Test
    void 데이터가_없는_카테고리는_커버리지에서_제외되고_비율이_줄어든다() {
        when(metricScoreMapper.findAllLatestScores()).thenReturn(List.of(
                dong(1L, "80", null),
                dong(2L, "50", null),
                dong(3L, "20", null)
        ));
        ConditionBundle condition = conditionWithWeights(Map.of("SAFETY", "1", "CONVENIENCE", "1"));

        List<RankedRecommendation> result = calculator.calculate(List.of(candidate(1L)), condition);

        RankedRecommendation recommendation = result.get(0);
        assertThat(recommendation.getCategoryBreakdowns()).hasSize(1);
        assertThat(recommendation.getDataCoverageRate()).isEqualByComparingTo("50.00");
    }

    @Test
    void 전체_행정동_점수가_동일하면_표준편차가_0이라_중립값_50점이다() {
        when(metricScoreMapper.findAllLatestScores()).thenReturn(List.of(
                dong(1L, "70", null),
                dong(2L, "70", null),
                dong(3L, "70", null)
        ));
        ConditionBundle condition = conditionWithWeights(Map.of("SAFETY", "1"));

        List<RankedRecommendation> result = calculator.calculate(List.of(candidate(1L)), condition);

        assertThat(result.get(0).getTotalScore()).isEqualByComparingTo("50.00");
    }

    @Test
    void 총점_내림차순으로_순위를_매긴다() {
        when(metricScoreMapper.findAllLatestScores()).thenReturn(List.of(
                dong(1L, "80", null),
                dong(2L, "50", null),
                dong(3L, "20", null)
        ));
        ConditionBundle condition = conditionWithWeights(Map.of("SAFETY", "1"));

        List<RankedRecommendation> result = calculator.calculate(
                List.of(candidate(3L), candidate(1L), candidate(2L)), condition);

        assertThat(result)
                .extracting(RankedRecommendation::getAdminDongId, RankedRecommendation::getRank)
                .containsExactly(tuple(1L, 1), tuple(2L, 2), tuple(3L, 3));
    }

    private Map<String, CategoryScoreBreakdown> breakdownByCategory(RankedRecommendation recommendation) {
        return recommendation.getCategoryBreakdowns().stream()
                .collect(Collectors.toMap(CategoryScoreBreakdown::getCategory, b -> b));
    }

    private ConditionBundle conditionWithWeights(Map<String, String> weights) {
        Map<String, BigDecimal> categoryWeights = weights.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, e -> new BigDecimal(e.getValue())));

        return ConditionBundle.builder()
                .categoryWeights(categoryWeights)
                .build();
    }

    private RecommendationCandidate candidate(Long adminDongId) {
        return RecommendationCandidate.builder().adminDongId(adminDongId).build();
    }

    private AdminDongMetricScores dong(Long adminDongId, String safetyScore, String cultureScore) {
        AdminDongMetricScores scores = new AdminDongMetricScores();
        scores.setAdminDongId(adminDongId);
        scores.setSafetyScore(safetyScore == null ? null : new BigDecimal(safetyScore));
        scores.setCultureScore(cultureScore == null ? null : new BigDecimal(cultureScore));

        return scores;
    }
}
