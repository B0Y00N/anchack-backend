package com.kbait.anchack.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;

/**
 * 정적 지표 점수 계산에서 공통으로 쓰는 통계 유틸리티.
 */
public final class ScoreMath {

    private ScoreMath() {
    }

    /**
     * min-max 정규화로 0~100 스케일로 변환한다.
     * 전체 값이 동일해서 범위가 0이면(range=0) 중립값 50을 반환한다.
     */
    public static double[] minMaxNormalize(double[] values) {
        double min = Arrays.stream(values).min().orElse(0);
        double max = Arrays.stream(values).max().orElse(0);
        double range = max - min;

        double[] result = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = range == 0 ? 50.0 : (values[i] - min) / range * 100.0;
        }

        return result;
    }

    /**
     * z-score(평균 0, 표준편차 1)로 표준화한다.
     * 표준편차가 0이면(전체 값이 동일) 중립값 0을 반환한다.
     */
    public static double[] zScore(double[] values) {
        double mean = Arrays.stream(values).average().orElse(0);

        double variance = Arrays.stream(values)
                .map(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);

        double stddev = Math.sqrt(variance);

        double[] result = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = stddev == 0 ? 0.0 : (values[i] - mean) / stddev;
        }

        return result;
    }

    public static double[] log1p(double[] values) {
        double[] result = new double[values.length];

        for (int i = 0; i < values.length; i++) {
            result[i] = Math.log1p(values[i]);
        }

        return result;
    }

    public static double clip(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    public static BigDecimal round2(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
