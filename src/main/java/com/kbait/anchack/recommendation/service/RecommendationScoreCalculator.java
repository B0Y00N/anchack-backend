package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.common.util.ScoreMath;
import com.kbait.anchack.recommendation.dto.AdminDongMetricScores;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.RankedRecommendation;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;
import com.kbait.anchack.recommendation.mapper.MetricScoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * 하드필터를 통과한 후보에 사용자 가중치(condition_weights)를 반영해 순위를 매긴다.
 *
 * 1) 전체 행정동의 카테고리별 최신 점수로 z-score 표준화한다(모집단 = 전체 행정동 -
 *    검색 조건과 무관하게 카테고리별 z-score는 고정되고, 최종 총점만 사용자별
 *    가중치에 따라 달라진다).
 * 2) 후보별로 정규화된 가중치 * z-score를 합산한 뒤, safety_score와 동일한 T-score
 *    변환(50 + 10 * raw, clip 0~100)으로 totalScore를 산출한다.
 * 3) 가중치를 준 카테고리 중 해당 후보에 데이터가 없는 비율을 dataCoverageRate로 반영한다.
 * 4) totalScore 내림차순으로 rank를 부여한다.
 *
 * 다른 구현체가 나올 여지가 없는 단일 계산 로직이라 인터페이스 분리 없이
 * 구현체만 둠.
 */
@Component
@RequiredArgsConstructor
public class RecommendationScoreCalculator {

    private static final Map<String, Function<AdminDongMetricScores, BigDecimal>> CATEGORY_EXTRACTORS = Map.of(
            "CULTURE", AdminDongMetricScores::getCultureScore,
            "TRANSIT", AdminDongMetricScores::getTransitScore,
            "SPORTS", AdminDongMetricScores::getSportsScore,
            "NATURE", AdminDongMetricScores::getNatureScore,
            "CONVENIENCE", AdminDongMetricScores::getConvenienceScore,
            "HEALTHCARE", AdminDongMetricScores::getHealthcareScore,
            "FOOD", AdminDongMetricScores::getFoodScore,
            "SAFETY", AdminDongMetricScores::getSafetyScore,
            "SILENCE", AdminDongMetricScores::getSilenceScore
    );

    private final MetricScoreMapper metricScoreMapper;

    public List<RankedRecommendation> calculate(List<RecommendationCandidate> candidates, ConditionBundle condition) {
        List<AdminDongMetricScores> allScores = metricScoreMapper.findAllLatestScores();
        Map<Long, AdminDongMetricScores> scoresByDong = toMapByDong(allScores);
        Map<String, Map<Long, Double>> zScoresByCategory = computeZScoresByCategory(allScores);
        Map<String, BigDecimal> normalizedWeights = normalizeWeights(condition.getCategoryWeights());

        List<RankedRecommendation> scored = candidates.stream()
                .map(candidate -> scoreCandidate(candidate, scoresByDong, zScoresByCategory, normalizedWeights))
                .toList();

        return assignRanks(scored);
    }

    private Map<Long, AdminDongMetricScores> toMapByDong(List<AdminDongMetricScores> allScores) {
        Map<Long, AdminDongMetricScores> result = new LinkedHashMap<>();
        allScores.forEach(row -> result.put(row.getAdminDongId(), row));

        return result;
    }

    private Map<String, Map<Long, Double>> computeZScoresByCategory(List<AdminDongMetricScores> allScores) {
        Map<String, Map<Long, Double>> result = new LinkedHashMap<>();
        CATEGORY_EXTRACTORS.forEach(
                (category, extractor) -> result.put(category, computeZScoresForCategory(allScores, extractor)));

        return result;
    }

    private Map<Long, Double> computeZScoresForCategory(
            List<AdminDongMetricScores> allScores,
            Function<AdminDongMetricScores, BigDecimal> extractor
    ) {
        List<AdminDongMetricScores> withData = allScores.stream()
                .filter(row -> extractor.apply(row) != null)
                .toList();

        double[] values = withData.stream().mapToDouble(row -> extractor.apply(row).doubleValue()).toArray();
        double[] zScores = ScoreMath.zScore(values);

        Map<Long, Double> result = new LinkedHashMap<>();
        for (int i = 0; i < withData.size(); i++) {
            result.put(withData.get(i).getAdminDongId(), zScores[i]);
        }

        return result;
    }

    private Map<String, BigDecimal> normalizeWeights(Map<String, BigDecimal> rawWeights) {
        BigDecimal sum = rawWeights.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, BigDecimal> result = new LinkedHashMap<>();
        rawWeights.forEach((category, weight) -> result.put(category, weight.divide(sum, 10, RoundingMode.HALF_UP)));

        return result;
    }

    private RankedRecommendation scoreCandidate(
            RecommendationCandidate candidate,
            Map<Long, AdminDongMetricScores> scoresByDong,
            Map<String, Map<Long, Double>> zScoresByCategory,
            Map<String, BigDecimal> normalizedWeights
    ) {
        List<CategoryScoreBreakdown> breakdowns = buildBreakdowns(
                candidate.getAdminDongId(), scoresByDong, zScoresByCategory, normalizedWeights);

        double rawTotal = breakdowns.stream().mapToDouble(b -> b.getWeightedScore().doubleValue()).sum();
        BigDecimal totalScore = ScoreMath.round2(ScoreMath.clip(50 + 10 * rawTotal, 0, 100));
        BigDecimal dataCoverageRate = calculateCoverageRate(breakdowns.size(), normalizedWeights.size());

        return RankedRecommendation.builder()
                .adminDongId(candidate.getAdminDongId())
                .totalScore(totalScore)
                .dataCoverageRate(dataCoverageRate)
                .commuteTime(candidate.getCommuteTime())
                .transferCount(candidate.getTransferCount())
                .route(candidate.getRoute())
                .transportType(candidate.getTransportType())
                .lineNum(candidate.getLineNum())
                .vehicleType(candidate.getVehicleType())
                .walkMin(candidate.getWalkMin())
                .transitMin(candidate.getTransitMin())
                .categoryBreakdowns(breakdowns)
                .build();
    }

    private List<CategoryScoreBreakdown> buildBreakdowns(
            Long adminDongId,
            Map<Long, AdminDongMetricScores> scoresByDong,
            Map<String, Map<Long, Double>> zScoresByCategory,
            Map<String, BigDecimal> normalizedWeights
    ) {
        List<CategoryScoreBreakdown> breakdowns = new ArrayList<>();

        normalizedWeights.forEach((category, weight) -> {
            Double zScore = zScoresByCategory.getOrDefault(category, Map.of()).get(adminDongId);

            if (zScore != null) {
                breakdowns.add(toBreakdown(category, weight, zScore, scoresByDong.get(adminDongId)));
            }
        });

        return breakdowns;
    }

    private CategoryScoreBreakdown toBreakdown(
            String category,
            BigDecimal weight,
            double zScore,
            AdminDongMetricScores scores
    ) {
        BigDecimal rawScore = CATEGORY_EXTRACTORS.get(category).apply(scores);
        BigDecimal weightedScore = ScoreMath.round2(weight.doubleValue() * zScore);

        return CategoryScoreBreakdown.builder()
                .category(category)
                .rawScore(rawScore)
                .weight(weight)
                .weightedScore(weightedScore)
                .build();
    }

    private BigDecimal calculateCoverageRate(int availableCount, int totalCount) {
        return ScoreMath.round2((double) availableCount / totalCount * 100);
    }

    private List<RankedRecommendation> assignRanks(List<RankedRecommendation> scored) {
        List<RankedRecommendation> sorted = scored.stream()
                .sorted(Comparator.comparing(RankedRecommendation::getTotalScore).reversed())
                .toList();

        List<RankedRecommendation> ranked = new ArrayList<>();
        for (int i = 0; i < sorted.size(); i++) {
            ranked.add(withRank(sorted.get(i), i + 1));
        }

        return ranked;
    }

    private RankedRecommendation withRank(RankedRecommendation recommendation, int rank) {
        return RankedRecommendation.builder()
                .adminDongId(recommendation.getAdminDongId())
                .totalScore(recommendation.getTotalScore())
                .dataCoverageRate(recommendation.getDataCoverageRate())
                .rank(rank)
                .commuteTime(recommendation.getCommuteTime())
                .transferCount(recommendation.getTransferCount())
                .route(recommendation.getRoute())
                .transportType(recommendation.getTransportType())
                .lineNum(recommendation.getLineNum())
                .vehicleType(recommendation.getVehicleType())
                .walkMin(recommendation.getWalkMin())
                .transitMin(recommendation.getTransitMin())
                .categoryBreakdowns(recommendation.getCategoryBreakdowns())
                .build();
    }
}
