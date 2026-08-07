package com.kbait.anchack.ingestion.normalizer;

import com.kbait.anchack.ingestion.client.MolitRentApiCategory;
import com.kbait.anchack.ingestion.domain.RentalTransaction;
import com.kbait.anchack.ingestion.dto.external.RawRentalTransaction;
import com.kbait.anchack.ingestion.exception.InvalidMolitRentDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class RentalTransactionNormalizerTest {

    private final RentalTransactionNormalizer normalizer = new RentalTransactionNormalizer();

    @Test
    void 오피스텔은_원본_주택유형이_없어도_오피스텔로_변환한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.OFFICETEL)
                .houseType(null)
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getHouseType()).isEqualTo("오피스텔");
    }

    @Test
    void 오피스텔은_전용면적을_사용한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.OFFICETEL)
                .exclusiveArea("16.34")
                .totalFloorArea("사용하지 않는 값")
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getArea()).isEqualTo(new BigDecimal("16.34"));
    }

    @Test
    void 연립_다세대는_전용면적을_사용한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .exclusiveArea("45.53")
                .totalFloorArea("사용하지 않는 값")
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getArea()).isEqualTo(new BigDecimal("45.53"));
    }

    @Test
    void 단독_다가구는_연면적을_사용한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.SINGLE_HOUSE)
                .exclusiveArea("사용하지 않는 값")
                .totalFloorArea("20")
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getArea()).isEqualTo(new BigDecimal("20.00"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"연립", "다세대", "연립다세대"})
    void 연립_다세대_API의_허용된_주택유형을_보존한다(String houseType) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .houseType(houseType)
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getHouseType()).isEqualTo(houseType);
    }

    @ParameterizedTest
    @ValueSource(strings = {"단독", "다가구"})
    void 단독_다가구_API의_허용된_주택유형을_보존한다(String houseType) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.SINGLE_HOUSE)
                .houseType(houseType)
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getHouseType()).isEqualTo(houseType);
    }

    @Test
    void 금액은_쉼표만_제거하고_만원_단위를_그대로_보존한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .deposit("22,422")
                .monthlyRent("31")
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getDeposit()).isEqualTo(22_422L);
        assertThat(result.getRent()).isEqualTo(31L);
        assertThat(result.getDeposit()).isNotEqualTo(224_220_000L);
        assertThat(result.getRent()).isNotEqualTo(310_000L);
    }

    @Test
    void DB에서_생성하거나_제공하지_않는_필드는_정해진_값으로_매핑한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE).build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getAdminDongId()).isNull();
        assertThat(result.getMaintenanceFee()).isZero();
    }

    @Test
    void 거래일_문자열을_LocalDate로_변환한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .dealYear(" 2026 ")
                .dealMonth(" 6 ")
                .dealDay(" 17 ")
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getTransactionDate()).isEqualTo(LocalDate.of(2026, 6, 17));
    }

    @Test
    void 구_코드와_법정동명의_앞뒤_공백을_제거한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .guCode(" 11620 ")
                .legalDongName(" 신림동 ")
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getGuCode()).isEqualTo("11620");
        assertThat(result.getLegalDongName()).isEqualTo("신림동");
    }

    @ParameterizedTest(name = "{0} -> {1}")
    @MethodSource("areaRoundingCases")
    void 면적을_HALF_UP으로_소수_둘째_자리까지_반올림한다(String source, String expected) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.OFFICETEL)
                .exclusiveArea(source)
                .build();

        RentalTransaction result = normalizer.normalize(rawTransaction);

        assertThat(result.getArea()).isEqualTo(new BigDecimal(expected));
        assertThat(result.getArea().scale()).isEqualTo(2);
    }

    @Test
    void 원본_거래가_null이면_예외가_발생한다() {
        InvalidMolitRentDataException exception = catchInvalid(() -> normalizer.normalize(null));

        assertThat(exception).hasMessageContaining("rawTransaction");
    }

    @Test
    void API_종류가_null이면_예외가_발생한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.OFFICETEL)
                .apiCategory(null)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("apiCategory");
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "1162", "1162A", "116200"})
    void 구_코드가_숫자_다섯_자리가_아니면_예외가_발생한다(String guCode) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .guCode(guCode)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("guCode");
    }

    @ParameterizedTest
    @MethodSource("invalidLegalDongNames")
    void 법정동명이_없거나_100자를_초과하면_예외가_발생한다(String legalDongName) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .legalDongName(legalDongName)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("legalDongName");
    }

    @Test
    void 계약연도가_누락되면_예외가_발생한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .dealYear(null)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("dealYear");
    }

    @Test
    void 계약월이_누락되면_예외가_발생한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .dealMonth(" ")
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("dealMonth");
    }

    @Test
    void 계약일이_누락되면_예외가_발생한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .dealDay(null)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("dealDay");
    }

    @Test
    void 거래일이_정수가_아니면_NumberFormatException을_보존한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .dealYear("20x6")
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("거래일").hasCauseExactlyInstanceOf(NumberFormatException.class);
    }

    @Test
    void 존재하지_않는_거래일이면_DateTimeException을_보존한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .dealMonth("2")
                .dealDay("30")
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("거래일").hasCauseInstanceOf(DateTimeException.class);
    }

    @ParameterizedTest
    @MethodSource("invalidDepositValues")
    void 보증금_형식이_올바르지_않으면_예외가_발생한다(String deposit) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .deposit(deposit)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("deposit");
    }

    @Test
    void 보증금이_long_범위를_초과하면_NumberFormatException을_보존한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .deposit("9223372036854775808")
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("deposit").hasCauseExactlyInstanceOf(NumberFormatException.class);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void 월세가_없으면_예외가_발생한다(String monthlyRent) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.ROW_HOUSE)
                .monthlyRent(monthlyRent)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("monthlyRent");
    }

    @ParameterizedTest
    @MethodSource("invalidSelectedAreas")
    void 선택한_면적이_없거나_숫자_형식이_아니면_예외가_발생한다(String area) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.OFFICETEL)
                .exclusiveArea(area)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("area");
    }

    @Test
    void 단독_다가구의_연면적이_null이면_예외가_발생한다() {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.SINGLE_HOUSE)
                .exclusiveArea("45.53")
                .totalFloorArea(null)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("area");
    }

    @ParameterizedTest
    @MethodSource("outOfRangeAreas")
    void 반올림한_면적이_DB_범위를_벗어나면_예외가_발생한다(String area, String messagePart) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(MolitRentApiCategory.OFFICETEL)
                .exclusiveArea(area)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("area").hasMessageContaining(messagePart);
    }

    @ParameterizedTest
    @MethodSource("mismatchedHouseTypes")
    void API_종류에_맞지_않는_주택유형이면_예외가_발생한다(
            MolitRentApiCategory apiCategory,
            String houseType
    ) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(apiCategory)
                .houseType(houseType)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("houseType").hasMessageContaining("허용");
    }

    @ParameterizedTest
    @MethodSource("missingHouseTypes")
    void 주택유형이_필요한_API에서_값이_없으면_예외가_발생한다(
            MolitRentApiCategory apiCategory,
            String houseType
    ) {
        RawRentalTransaction rawTransaction = validTransactionBuilder(apiCategory)
                .houseType(houseType)
                .build();

        InvalidMolitRentDataException exception = catchInvalid(
                () -> normalizer.normalize(rawTransaction)
        );

        assertThat(exception).hasMessageContaining("houseType");
    }

    private RawRentalTransaction.RawRentalTransactionBuilder validTransactionBuilder(
            MolitRentApiCategory apiCategory
    ) {
        RawRentalTransaction.RawRentalTransactionBuilder builder = RawRentalTransaction.builder()
                .apiCategory(apiCategory)
                .guCode("11620")
                .legalDongName("신림동")
                .dealYear("2026")
                .dealMonth("6")
                .dealDay("17")
                .exclusiveArea("45.53")
                .totalFloorArea("20")
                .deposit("22,422")
                .monthlyRent("31");

        if (apiCategory == MolitRentApiCategory.ROW_HOUSE) {
            return builder.houseType("다세대");
        }
        if (apiCategory == MolitRentApiCategory.SINGLE_HOUSE) {
            return builder.houseType("다가구");
        }

        return builder.houseType(null);
    }

    private InvalidMolitRentDataException catchInvalid(Runnable action) {
        Throwable exception = catchThrowable(action::run);

        assertThat(exception).isExactlyInstanceOf(InvalidMolitRentDataException.class);
        return (InvalidMolitRentDataException) exception;
    }

    private static Stream<Arguments> areaRoundingCases() {
        return Stream.of(
                Arguments.of("12.075", "12.08"),
                Arguments.of("11.834", "11.83"),
                Arguments.of("40.245", "40.25"),
                Arguments.of("15.106", "15.11"),
                Arguments.of("19.264", "19.26"),
                Arguments.of("26.625", "26.63"),
                Arguments.of("45.53", "45.53"),
                Arguments.of("20", "20.00")
        );
    }

    private static Stream<String> invalidLegalDongNames() {
        return Stream.of(null, " ", "가".repeat(101));
    }

    private static Stream<String> invalidDepositValues() {
        return Stream.of(null, "", " ", "22,42", "1,00", "1,,000", "-1", "1.5", "abc");
    }

    private static Stream<String> invalidSelectedAreas() {
        return Stream.of(null, "", " ", "abc", "1,000", "-1");
    }

    private static Stream<Arguments> outOfRangeAreas() {
        return Stream.of(
                Arguments.of("0", "0보다"),
                Arguments.of("0.004", "0보다"),
                Arguments.of("999.995", "999.99")
        );
    }

    private static Stream<Arguments> mismatchedHouseTypes() {
        return Stream.of(
                Arguments.of(MolitRentApiCategory.ROW_HOUSE, "단독"),
                Arguments.of(MolitRentApiCategory.SINGLE_HOUSE, "다세대")
        );
    }

    private static Stream<Arguments> missingHouseTypes() {
        return Stream.of(
                Arguments.of(MolitRentApiCategory.ROW_HOUSE, null),
                Arguments.of(MolitRentApiCategory.ROW_HOUSE, " "),
                Arguments.of(MolitRentApiCategory.SINGLE_HOUSE, null),
                Arguments.of(MolitRentApiCategory.SINGLE_HOUSE, " ")
        );
    }
}
