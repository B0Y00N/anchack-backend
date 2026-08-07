package com.kbait.anchack.metric.service;

import com.kbait.anchack.metric.dto.DensityScoreInput;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DensityMinMaxScoreCalculatorTest {

    private final DensityMinMaxScoreCalculator calculator = new DensityMinMaxScoreCalculator();

    @Test
    void singleCountLowestDensityGetsZeroAndHighestGetsHundred() {
        List<DensityScoreInput> inputs = List.of(
                input(1L, Map.of("culture", 0L)),
                input(2L, Map.of("culture", 50L)),
                input(3L, Map.of("culture", 100L))
        );

        Map<Long, BigDecimal> result = calculator.calculate(inputs);

        assertThat(result.get(1L)).isEqualByComparingTo("0.00");
        assertThat(result.get(3L)).isEqualByComparingTo("100.00");
    }

    @Test
    void allDongsWithSameCountGetNeutralFiftyScore() {
        List<DensityScoreInput> inputs = List.of(
                input(1L, Map.of("culture", 10L)),
                input(2L, Map.of("culture", 10L))
        );

        Map<Long, BigDecimal> result = calculator.calculate(inputs);

        assertThat(result.get(1L)).isEqualByComparingTo("50.00");
        assertThat(result.get(2L)).isEqualByComparingTo("50.00");
    }

    @Test
    void zeroPopulationOrAreaTreatsDensityAsZero() {
        List<DensityScoreInput> inputs = List.of(
                DensityScoreInput.builder()
                        .adminDongId(1L)
                        .counts(Map.of("culture", 10L))
                        .dongPopulation(BigDecimal.ZERO)
                        .dongArea(BigDecimal.valueOf(1))
                        .build(),
                input(2L, Map.of("culture", 10L))
        );

        Map<Long, BigDecimal> result = calculator.calculate(inputs);

        // 1번은 인구밀도 0점 + 면적밀도 50점(둘 다 count=10이라 동일) 평균 = 25점
        assertThat(result.get(1L)).isEqualByComparingTo("25.00");
    }

    @Test
    void multipleCountsAverageScorePerCategory() {
        // mart는 1번이 최대(100점), bank는 2번이 최대(100점) -> 서로 다른 항목이 각자 1등을 가짐
        List<DensityScoreInput> inputs = List.of(
                input(1L, Map.of("mart", 100L, "bank", 0L)),
                input(2L, Map.of("mart", 0L, "bank", 100L))
        );

        Map<Long, BigDecimal> result = calculator.calculate(inputs);

        // 합산했다면 두 행정동이 동점(100)이었겠지만, 항목별 정규화 후 평균이라
        // mart=100/bank=0 -> mart점수 100, bank점수 0 -> 평균 50. 2번도 대칭이라 동일하게 50.
        assertThat(result.get(1L)).isEqualByComparingTo("50.00");
        assertThat(result.get(2L)).isEqualByComparingTo("50.00");
    }

    private DensityScoreInput input(Long adminDongId, Map<String, Long> counts) {
        return DensityScoreInput.builder()
                .adminDongId(adminDongId)
                .counts(counts)
                .dongPopulation(BigDecimal.valueOf(10000))
                .dongArea(BigDecimal.valueOf(1))
                .build();
    }
}
