package com.kbait.anchack.metric.service;

import com.kbait.anchack.metric.dto.SafetyScoreInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SafetyScoreCalculatorTest {

    private final SafetyScoreCalculator calculator = new SafetyScoreCalculator();

    @Test
    void lowerCrimeRateAndMoreSafetyFacilitiesGetHigherScore() {
        SafetyScoreInput safeDong = SafetyScoreInput.builder()
                .adminDongId(1L)
                .dongPopulation(BigDecimal.valueOf(10000))
                .cctvCount(50)
                .streetLightCount(100)
                .policeOfficeCount(5)
                .safetyBellCount(20)
                .crimeRatePer10k(BigDecimal.valueOf(2))
                .build();

        SafetyScoreInput dangerousDong = SafetyScoreInput.builder()
                .adminDongId(2L)
                .dongPopulation(BigDecimal.valueOf(10000))
                .cctvCount(5)
                .streetLightCount(10)
                .policeOfficeCount(1)
                .safetyBellCount(2)
                .crimeRatePer10k(BigDecimal.valueOf(20))
                .build();

        Map<Long, BigDecimal> result = calculator.calculate(List.of(safeDong, dangerousDong));

        assertThat(result.get(1L)).isGreaterThan(result.get(2L));
    }

    @Test
    void scoreIsClippedBetweenZeroAndHundred() {
        SafetyScoreInput input = SafetyScoreInput.builder()
                .adminDongId(1L)
                .dongPopulation(BigDecimal.valueOf(10000))
                .cctvCount(0)
                .streetLightCount(0)
                .policeOfficeCount(0)
                .safetyBellCount(0)
                .crimeRatePer10k(BigDecimal.valueOf(1000))
                .build();

        Map<Long, BigDecimal> result = calculator.calculate(List.of(input));

        assertThat(result.get(1L)).isBetween(BigDecimal.ZERO, BigDecimal.valueOf(100));
    }

    @Test
    void singleDongGetsNeutralFiftyScoreDueToZeroStdDev() {
        SafetyScoreInput input = SafetyScoreInput.builder()
                .adminDongId(1L)
                .dongPopulation(BigDecimal.valueOf(10000))
                .cctvCount(10)
                .streetLightCount(10)
                .policeOfficeCount(10)
                .safetyBellCount(10)
                .crimeRatePer10k(BigDecimal.valueOf(5))
                .build();

        Map<Long, BigDecimal> result = calculator.calculate(List.of(input));

        assertThat(result.get(1L)).isEqualByComparingTo("50.00");
    }
}
