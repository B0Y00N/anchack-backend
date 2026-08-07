package com.kbait.anchack.metric.service;

import com.kbait.anchack.metric.dto.SafetyScoreInput;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 치안 지표 점수 계산.
 *
 * 1) CCTV/가로등/경찰서/안심벨: 인구 1만명당 정규화 -> log1p 변환 -> z-score 표준화
 *    (분포가 크게 치우쳐 있어 log1p로 이상치를 완충한 뒤 표준화)
 * 2) 범죄율(gu_crime_stats.crime_rate): 이미 인구 1만명당 비율이므로 원값 그대로 z-score 표준화
 * 3) 가중합: crime 40%(음의 방향, 범죄율이 높을수록 감점) + cctv 20% + streetlight 15%
 *    + police 15% + bell 10%
 * 4) safety_score = clip(50 + 10 * raw, 0, 100), 소수 둘째자리 반올림
 *
 * 반드시 "전체 행정동" 데이터를 한 번에 넘겨야 z-score 기준(평균/표준편차)이 올바르게 계산된다.
 */
@Component
public class SafetyScoreCalculator {

    private static final double POPULATION_BASE = 10_000.0;

    private static final double CRIME_WEIGHT = 0.40;
    private static final double CCTV_WEIGHT = 0.20;
    private static final double STREETLIGHT_WEIGHT = 0.15;
    private static final double POLICE_WEIGHT = 0.15;
    private static final double BELL_WEIGHT = 0.10;

    public Map<Long, BigDecimal> calculate(List<SafetyScoreInput> inputs) {

        int size = inputs.size();

        double[] cctvPer10k = new double[size];
        double[] streetLightPer10k = new double[size];
        double[] policePer10k = new double[size];
        double[] bellPer10k = new double[size];
        double[] crimeRate = new double[size];

        for (int i = 0; i < size; i++) {
            SafetyScoreInput input = inputs.get(i);
            double population = input.getDongPopulation() == null ? 0.0 : input.getDongPopulation().doubleValue();

            cctvPer10k[i] = perPopulation(input.getCctvCount(), population);
            streetLightPer10k[i] = perPopulation(input.getStreetLightCount(), population);
            policePer10k[i] = perPopulation(input.getPoliceOfficeCount(), population);
            bellPer10k[i] = perPopulation(input.getSafetyBellCount(), population);
            crimeRate[i] = input.getCrimeRatePer10k() == null ? 0.0 : input.getCrimeRatePer10k().doubleValue();
        }

        double[] zCctv = ScoreMath.zScore(ScoreMath.log1p(cctvPer10k));
        double[] zStreetLight = ScoreMath.zScore(ScoreMath.log1p(streetLightPer10k));
        double[] zPolice = ScoreMath.zScore(ScoreMath.log1p(policePer10k));
        double[] zBell = ScoreMath.zScore(ScoreMath.log1p(bellPer10k));
        double[] zCrime = ScoreMath.zScore(crimeRate);

        Map<Long, BigDecimal> result = new LinkedHashMap<>();

        for (int i = 0; i < size; i++) {
            double raw =
                    -CRIME_WEIGHT * zCrime[i]
                            + CCTV_WEIGHT * zCctv[i]
                            + STREETLIGHT_WEIGHT * zStreetLight[i]
                            + POLICE_WEIGHT * zPolice[i]
                            + BELL_WEIGHT * zBell[i];

            double score = ScoreMath.clip(50 + 10 * raw, 0, 100);

            result.put(inputs.get(i).getAdminDongId(), ScoreMath.round2(score));
        }

        return result;
    }

    private double perPopulation(Integer count, double population) {
        if (count == null || population == 0) {
            return 0.0;
        }

        return count / population * POPULATION_BASE;
    }
}
