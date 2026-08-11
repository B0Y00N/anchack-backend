package com.kbait.anchack.rental.service;

import com.kbait.anchack.rental.domain.RentalTransaction;
import com.kbait.anchack.rental.domain.RentalTransactionCategoryCounts;
import com.kbait.anchack.rental.exception.UnsafeMolitRentReplacementException;
import com.kbait.anchack.rental.mapper.RentalTransactionMapper;
import com.kbait.anchack.rental.service.impl.RentalTransactionWriteServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RentalTransactionWriteServiceTest {

    private static final String GU_CODE = "11620";
    private static final YearMonth JUNE_2026 = YearMonth.of(2026, 6);
    private static final LocalDate JUNE_START = LocalDate.of(2026, 6, 1);
    private static final LocalDate JULY_START = LocalDate.of(2026, 7, 1);
    private static final RentalTransactionCategoryCounts ZERO_COUNTS =
            new RentalTransactionCategoryCounts(0, 0, 0);

    @Mock
    private RentalTransactionMapper rentalTransactionMapper;

    private RentalTransactionWriteService writeService;

    @BeforeEach
    void setUp() {
        writeService = new RentalTransactionWriteServiceImpl(rentalTransactionMapper);
    }

    @Test
    void 빈_목록이어도_월_범위를_삭제하고_INSERT는_호출하지_않는다() {
        stubExistingCounts(JUNE_2026, ZERO_COUNTS);

        writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, List.of(), ZERO_COUNTS);

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verifyNoMoreInteractions();
        verify(rentalTransactionMapper, never()).insertBatch(anyList());
    }

    @Test
    void 기존이_0건이고_신규가_양수이면_조회_후_교체한다() {
        List<RentalTransaction> transactions = createTransactions(1);
        RentalTransactionCategoryCounts replacementCounts = categoryCounts(1);
        stubExistingCounts(JUNE_2026, ZERO_COUNTS);

        writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, transactions, replacementCounts);

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).insertBatch(transactions);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void 기존과_신규가_모두_양수이면_조회_후_교체한다() {
        List<RentalTransaction> transactions = createTransactions(3);
        RentalTransactionCategoryCounts existingCounts = new RentalTransactionCategoryCounts(3, 2, 1);
        RentalTransactionCategoryCounts replacementCounts = new RentalTransactionCategoryCounts(1, 1, 1);
        stubExistingCounts(JUNE_2026, existingCounts);

        writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, transactions, replacementCounts);

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).insertBatch(transactions);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void 기존_100건에서_신규_1건으로_감소해도_조회_후_교체한다() {
        List<RentalTransaction> transactions = createTransactions(1);
        RentalTransactionCategoryCounts existingCounts = categoryCounts(100);
        RentalTransactionCategoryCounts replacementCounts = categoryCounts(1);
        stubExistingCounts(JUNE_2026, existingCounts);

        writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, transactions, replacementCounts);

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).insertBatch(transactions);
        inOrder.verifyNoMoreInteractions();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("unsafeReplacementCounts")
    void 기존이_양수이고_같은_API_유형의_신규가_0건이면_전체_교체를_차단한다(
            String caseName,
            RentalTransactionCategoryCounts existingCounts,
            RentalTransactionCategoryCounts replacementCounts
    ) {
        List<RentalTransaction> transactions = createTransactions((int) replacementCounts.totalCount());
        stubExistingCounts(JUNE_2026, existingCounts);

        Throwable actual = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(
                        GU_CODE,
                        JUNE_2026,
                        transactions,
                        replacementCounts
                )
        );

        assertThat(actual)
                .isExactlyInstanceOf(UnsafeMolitRentReplacementException.class)
                .hasMessageContaining(GU_CODE)
                .hasMessageContaining(JUNE_2026.toString());
        verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        verify(rentalTransactionMapper, never()).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        verify(rentalTransactionMapper, never()).insertBatch(anyList());
        verifyNoMoreInteractions(rentalTransactionMapper);
    }

    @Test
    void 거래_500건은_삭제_후_순서를_유지해_한_번_INSERT한다() {
        List<RentalTransaction> transactions = createTransactions(500);
        ArgumentCaptor<List<RentalTransaction>> captor = transactionListCaptor();
        stubExistingCounts(JUNE_2026, ZERO_COUNTS);

        writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, transactions, categoryCounts(500));

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).insertBatch(captor.capture());
        inOrder.verifyNoMoreInteractions();
        assertThat(captor.getValue()).hasSize(500).containsExactlyElementsOf(transactions);
        assertDepositOrder(captor.getValue(), 500);
    }

    @Test
    void 거래_501건은_삭제_후_500건과_1건으로_나누어_INSERT한다() {
        List<RentalTransaction> transactions = createTransactions(501);
        ArgumentCaptor<List<RentalTransaction>> captor = transactionListCaptor();
        stubExistingCounts(JUNE_2026, ZERO_COUNTS);

        writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, transactions, categoryCounts(501));

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper, times(2)).insertBatch(captor.capture());
        inOrder.verifyNoMoreInteractions();

        List<List<RentalTransaction>> chunks = captor.getAllValues();
        assertThat(chunks).hasSize(2);
        assertThat(chunks.get(0)).hasSize(500);
        assertThat(chunks.get(1)).hasSize(1);

        List<RentalTransaction> combined = new ArrayList<>();
        combined.addAll(chunks.get(0));
        combined.addAll(chunks.get(1));
        assertThat(combined).containsExactlyElementsOf(transactions);
        assertDepositOrder(combined, 501);
    }

    @Test
    void 거래_1000건은_빈_chunk_없이_500건씩_두_번_INSERT한다() {
        List<RentalTransaction> transactions = createTransactions(1_000);
        ArgumentCaptor<List<RentalTransaction>> captor = transactionListCaptor();
        stubExistingCounts(JUNE_2026, ZERO_COUNTS);

        writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, transactions, categoryCounts(1_000));

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper, times(2)).insertBatch(captor.capture());
        inOrder.verifyNoMoreInteractions();
        assertThat(captor.getAllValues()).hasSize(2).allSatisfy(chunk -> assertThat(chunk).hasSize(500));
    }

    @Test
    void 십이월_거래는_다음_연도_1월_1일을_삭제_종료일로_사용한다() {
        YearMonth december2026 = YearMonth.of(2026, 12);
        stubExistingCounts(december2026, ZERO_COUNTS);

        writeService.replaceMonthlyTransactions(
                GU_CODE,
                december2026,
                List.of(),
                ZERO_COUNTS
        );

        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2027, 1, 1)
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                LocalDate.of(2026, 12, 1),
                LocalDate.of(2027, 1, 1)
        );
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void INSERT_예외는_동일한_객체로_그대로_전파한다() {
        List<RentalTransaction> transactions = createTransactions(1);
        RuntimeException insertException = new RuntimeException("INSERT_FAILURE");
        stubExistingCounts(JUNE_2026, ZERO_COUNTS);
        when(rentalTransactionMapper.insertBatch(anyList())).thenThrow(insertException);

        Throwable actual = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, transactions, categoryCounts(1))
        );

        assertThat(actual).isSameAs(insertException);
        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).insertBatch(anyList());
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void 삭제_예외는_그대로_전파하고_INSERT를_호출하지_않는다() {
        RuntimeException deleteException = new RuntimeException("DELETE_FAILURE");
        stubExistingCounts(JUNE_2026, ZERO_COUNTS);
        when(rentalTransactionMapper.deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        )).thenThrow(deleteException);

        Throwable actual = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(
                        GU_CODE,
                        JUNE_2026,
                        createTransactions(1),
                        categoryCounts(1)
                )
        );

        assertThat(actual).isSameAs(deleteException);
        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        inOrder.verifyNoMoreInteractions();
        verify(rentalTransactionMapper, never()).insertBatch(anyList());
    }

    @Test
    void 기존_건수_조회_결과가_null이면_교체_SQL을_호출하지_않는다() {
        when(rentalTransactionMapper.findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        )).thenReturn(null);

        Throwable actual = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(
                        GU_CODE,
                        JUNE_2026,
                        List.of(),
                        ZERO_COUNTS
                )
        );

        assertThat(actual).isExactlyInstanceOf(IllegalStateException.class);
        verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        verify(rentalTransactionMapper, never()).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        verify(rentalTransactionMapper, never()).insertBatch(anyList());
        verifyNoMoreInteractions(rentalTransactionMapper);
    }

    @Test
    void 기존_건수_조회_예외는_동일한_객체로_전파하고_교체_SQL을_호출하지_않는다() {
        RuntimeException findException = new RuntimeException("FIND_COUNTS_FAILURE");
        when(rentalTransactionMapper.findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        )).thenThrow(findException);

        Throwable actual = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(
                        GU_CODE,
                        JUNE_2026,
                        List.of(),
                        ZERO_COUNTS
                )
        );

        assertThat(actual).isSameAs(findException);
        verify(rentalTransactionMapper).findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        verify(rentalTransactionMapper, never()).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                JUNE_START,
                JULY_START
        );
        verify(rentalTransactionMapper, never()).insertBatch(anyList());
        verifyNoMoreInteractions(rentalTransactionMapper);
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidInputs")
    void 잘못된_입력은_Mapper_호출_전에_거부한다(
            String caseName,
            String guCode,
            YearMonth dealYearMonth,
            List<RentalTransaction> transactions,
            RentalTransactionCategoryCounts categoryCounts
    ) {
        Throwable exception = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(
                        guCode,
                        dealYearMonth,
                        transactions,
                        categoryCounts
                )
        );

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(rentalTransactionMapper);
    }

    @Test
    void API_유형별_신규_건수가_null이면_Mapper_호출_전에_거부한다() {
        Throwable exception = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(GU_CODE, JUNE_2026, List.of(), null)
        );

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(rentalTransactionMapper);
    }

    @Test
    void API_유형별_건수_합계와_거래_목록_크기가_다르면_Mapper_호출_전에_거부한다() {
        Throwable exception = catchThrowable(
                () -> writeService.replaceMonthlyTransactions(
                        GU_CODE,
                        JUNE_2026,
                        createTransactions(1),
                        ZERO_COUNTS
                )
        );

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(rentalTransactionMapper);
    }

    private List<RentalTransaction> createTransactions(int count) {
        return IntStream.range(0, count)
                .mapToObj(index -> RentalTransaction.builder()
                        .adminDongId(null)
                        .guCode(GU_CODE)
                        .legalDongName("신림동")
                        .transactionDate(LocalDate.of(2026, 6, 17))
                        .houseType("다세대")
                        .area(new BigDecimal("45.53"))
                        .deposit(index)
                        .rent(31L)
                        .maintenanceFee(0)
                        .build())
                .toList();
    }

    private void assertDepositOrder(List<RentalTransaction> transactions, int count) {
        List<Long> expectedDeposits = IntStream.range(0, count)
                .mapToObj(index -> (long) index)
                .toList();

        assertThat(transactions)
                .extracting(RentalTransaction::getDeposit)
                .containsExactlyElementsOf(expectedDeposits);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<RentalTransaction>> transactionListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private RentalTransactionCategoryCounts categoryCounts(long rowHouseCount) {
        return new RentalTransactionCategoryCounts(0, rowHouseCount, 0);
    }

    private void stubExistingCounts(
            YearMonth dealYearMonth,
            RentalTransactionCategoryCounts existingCounts
    ) {
        when(rentalTransactionMapper.findCategoryCountsByGuCodeAndTransactionDateRange(
                GU_CODE,
                dealYearMonth.atDay(1),
                dealYearMonth.plusMonths(1).atDay(1)
        )).thenReturn(existingCounts);
    }

    private static Stream<Arguments> unsafeReplacementCounts() {
        return Stream.of(
                Arguments.of(
                        "오피스텔 신규 0건",
                        new RentalTransactionCategoryCounts(1, 0, 0),
                        new RentalTransactionCategoryCounts(0, 1, 0)
                ),
                Arguments.of(
                        "연립·다세대 신규 0건",
                        new RentalTransactionCategoryCounts(0, 1, 0),
                        new RentalTransactionCategoryCounts(1, 0, 0)
                ),
                Arguments.of(
                        "단독·다가구 신규 0건",
                        new RentalTransactionCategoryCounts(0, 0, 1),
                        new RentalTransactionCategoryCounts(0, 1, 0)
                )
        );
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of("guCode null", null, JUNE_2026, List.of(), ZERO_COUNTS),
                Arguments.of("guCode 숫자 5자리 아님", "1162A", JUNE_2026, List.of(), ZERO_COUNTS),
                Arguments.of("dealYearMonth null", GU_CODE, null, List.of(), ZERO_COUNTS),
                Arguments.of("transactions null", GU_CODE, JUNE_2026, null, ZERO_COUNTS)
        );
    }
}
