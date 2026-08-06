package com.kbait.anchack.ingestion.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RentalTransactionCategoryCountsTest {

    @Test
    void 모든_API가_0건인_건수를_생성할_수_있다() {
        RentalTransactionCategoryCounts counts = new RentalTransactionCategoryCounts(0, 0, 0);

        assertThat(counts.getOfficetelCount()).isZero();
        assertThat(counts.getRowHouseCount()).isZero();
        assertThat(counts.getSingleHouseCount()).isZero();
    }

    @Test
    void API_유형별_양수_건수를_보존한다() {
        RentalTransactionCategoryCounts counts = new RentalTransactionCategoryCounts(2, 1, 3);

        assertThat(counts.getOfficetelCount()).isEqualTo(2);
        assertThat(counts.getRowHouseCount()).isEqualTo(1);
        assertThat(counts.getSingleHouseCount()).isEqualTo(3);
    }

    @Test
    void 세_API_건수의_합계를_반환한다() {
        RentalTransactionCategoryCounts counts = new RentalTransactionCategoryCounts(2, 1, 3);

        assertThat(counts.totalCount()).isEqualTo(6);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("negativeCounts")
    void API_유형별_건수가_음수이면_거부한다(
            String caseName,
            long officetelCount,
            long rowHouseCount,
            long singleHouseCount,
            String fieldName
    ) {
        assertThatThrownBy(
                () -> new RentalTransactionCategoryCounts(officetelCount, rowHouseCount, singleHouseCount)
        )
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(fieldName);
    }

    private static Stream<Arguments> negativeCounts() {
        return Stream.of(
                Arguments.of("오피스텔 음수", -1, 0, 0, "officetelCount"),
                Arguments.of("연립·다세대 음수", 0, -1, 0, "rowHouseCount"),
                Arguments.of("단독·다가구 음수", 0, 0, -1, "singleHouseCount")
        );
    }
}
