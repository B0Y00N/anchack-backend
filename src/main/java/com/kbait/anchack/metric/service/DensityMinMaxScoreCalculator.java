package com.kbait.anchack.metric.service;

import com.kbait.anchack.metric.dto.DensityScoreInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.List;
import java.util.Set;

/**
 * 소음·치안을 제외한 정적 지표 공통 계산 로직.
 *
 * 1) 카운트 항목별로 각각:
 *    - 인구 대비 밀도: count / dong_population * 10000
 *    - 면적 대비 밀도: count / dong_area
 *    - 두 밀도를 전체 행정동 기준 min-max 정규화(0~100)한 뒤 평균 -> 그 항목의 점수
 * 2) 카운트 항목이 여러 개면(예: mart/bank/department_store) 항목별 점수를 다시 평균 -> 최종 점수
 *
 * 카운트를 먼저 합산하지 않고 항목별로 독립 정규화하는 이유: 항목마다 자연 발생 빈도가 달라서
 * (예: 마트가 은행보다 훨씬 흔함) 합산 후 정규화하면 빈도가 큰 항목이 결과를 지배해버린다.
 * 치안 점수에서 crime_rate/cctv_count 등을 각각 독립적으로 표준화하는 것과 같은 이유.
 *
 * 반드시 "전체 행정동" 데이터를 한 번에 넘겨야 정규화 기준(min/max)이 올바르게 계산된다.
 */
@Component
public class DensityMinMaxScoreCalculator {

    private static final double POPULATION_BASE = 10_000.0;

    public Map<Long, BigDecimal> calculate(List<DensityScoreInput> inputs) {

        if (inputs.isEmpty()) {
            return Map.of();
        }

        int size = inputs.size();
        Set<String> countKeys = inputs.get(0).getCounts().keySet();

        double[] scoreSum = new double[size];

        for (String countKey : countKeys) {
            double[] populationDensity = new double[size];
            double[] areaDensity = new double[size];

            for (int i = 0; i < size; i++) {
                DensityScoreInput input = inputs.get(i);
                long count = input.getCounts().getOrDefault(countKey, 0L);

                populationDensity[i] = density(count, input.getDongPopulation(), POPULATION_BASE);
                areaDensity[i] = density(count, input.getDongArea(), 1.0);
            }

            double[] populationScore = ScoreMath.minMaxNormalize(populationDensity);
            double[] areaScore = ScoreMath.minMaxNormalize(areaDensity);

            for (int i = 0; i < size; i++) {
                scoreSum[i] += (populationScore[i] + areaScore[i]) / 2.0;
            }
        }

        Map<Long, BigDecimal> result = new LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            double finalScore = scoreSum[i] / countKeys.size();
            result.put(inputs.get(i).getAdminDongId(), ScoreMath.round2(finalScore));
        }

        return result;
    }

    private double density(long count, BigDecimal denominator, double multiplier) {
        if (denominator == null || denominator.doubleValue() == 0) {
            return 0.0;
        }

        return count / denominator.doubleValue() * multiplier;
    }
}
