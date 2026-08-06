package com.kbait.anchack.metric.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;

class ScoreMathTest {

    @Test
    void minMaxNormalizesMinToZeroAndMaxToHundred() {
        double[] result = ScoreMath.minMaxNormalize(new double[]{0, 5, 10});

        assertThat(result).containsExactly(new double[]{0.0, 50.0, 100.0}, within(0.0001));
    }

    @Test
    void minMaxReturnsNeutralFiftyWhenAllValuesEqual() {
        double[] result = ScoreMath.minMaxNormalize(new double[]{7, 7, 7});

        assertThat(result).containsExactly(new double[]{50.0, 50.0, 50.0}, within(0.0001));
    }

    @Test
    void zScoreStandardizesToMeanZeroAndStdDevOne() {
        double[] result = ScoreMath.zScore(new double[]{2, 4, 4, 4, 5, 5, 7, 9});

        double mean = 0;
        for (double v : result) {
            mean += v;
        }
        mean /= result.length;

        assertThat(mean).isCloseTo(0.0, within(0.0001));
    }

    @Test
    void clipClampsValueToBoundsWhenOutOfRange() {
        assertThat(ScoreMath.clip(-10, 0, 100)).isEqualTo(0.0);
        assertThat(ScoreMath.clip(150, 0, 100)).isEqualTo(100.0);
        assertThat(ScoreMath.clip(50, 0, 100)).isEqualTo(50.0);
    }
}
