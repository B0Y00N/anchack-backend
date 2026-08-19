package com.kbait.anchack.rental.service;

import com.kbait.anchack.rental.client.MolitRentApiCategory;
import com.kbait.anchack.rental.client.MolitRentApiClient;
import com.kbait.anchack.rental.domain.RentalTransaction;
import com.kbait.anchack.rental.domain.RentalTransactionCategoryCounts;
import com.kbait.anchack.rental.dto.external.RawRentalTransaction;
import com.kbait.anchack.rental.exception.InvalidMolitRentDataException;
import com.kbait.anchack.rental.exception.MolitRentApiException;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import com.kbait.anchack.rental.normalizer.RentalTransactionNormalizer;
import com.kbait.anchack.rental.resolver.RentalAdminDongResolution;
import com.kbait.anchack.rental.resolver.RentalAdminDongResolver;
import com.kbait.anchack.rental.service.impl.MolitRentIngestionServiceImpl;
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
import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MolitRentIngestionServiceTest {

    private static final String GU_CODE = "11620";
    private static final YearMonth DEAL_YEAR_MONTH = YearMonth.of(2026, 6);

    @Mock
    private MolitRentApiClient molitRentApiClient;

    @Mock
    private RentalTransactionNormalizer rentalTransactionNormalizer;

    @Mock
    private RentalTransactionWriteService rentalTransactionWriteService;

    @Mock
    private RentalAdminDongResolver rentalAdminDongResolver;

    @Mock
    private RentalAdminDongResolver.ResolutionSession resolutionSession;

    private MolitRentIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new MolitRentIngestionServiceImpl(
                molitRentApiClient,
                rentalTransactionNormalizer,
                rentalTransactionWriteService,
                rentalAdminDongResolver
        );
        lenient().when(rentalAdminDongResolver.openSession()).thenReturn(resolutionSession);
        lenient().when(resolutionSession.resolve(any())).thenReturn(
                RentalAdminDongResolution.unmapped(RentalAdminDongResolution.Status.JIBUN_MISSING)
        );
    }

    @Test
    void 세_API를_모두_수집한_뒤_순서대로_정규화하고_한_번_저장한다() {
        RawRentalTransaction officetelFirst = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        RawRentalTransaction officetelSecond = createRawTransaction(MolitRentApiCategory.OFFICETEL, 2);
        RawRentalTransaction rowHouse = createRawTransaction(MolitRentApiCategory.ROW_HOUSE, 3);
        RawRentalTransaction singleHouseFirst = createRawTransaction(MolitRentApiCategory.SINGLE_HOUSE, 4);
        RawRentalTransaction singleHouseSecond = createRawTransaction(MolitRentApiCategory.SINGLE_HOUSE, 5);
        List<RawRentalTransaction> rawTransactions = List.of(
                officetelFirst,
                officetelSecond,
                rowHouse,
                singleHouseFirst,
                singleHouseSecond
        );
        List<RentalTransaction> normalizedTransactions = List.of(
                createRentalTransaction(MolitRentApiCategory.OFFICETEL, 1),
                createRentalTransaction(MolitRentApiCategory.OFFICETEL, 2),
                createRentalTransaction(MolitRentApiCategory.ROW_HOUSE, 3),
                createRentalTransaction(MolitRentApiCategory.SINGLE_HOUSE, 4),
                createRentalTransaction(MolitRentApiCategory.SINGLE_HOUSE, 5)
        );
        stubApiResults(rawTransactions);
        stubNormalizationResults(rawTransactions, normalizedTransactions);
        ArgumentCaptor<List<RentalTransaction>> transactionCaptor = transactionListCaptor();
        ArgumentCaptor<RentalTransactionCategoryCounts> categoryCountsCaptor = categoryCountsCaptor();

        ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);

        InOrder inOrder = inOrder(
                molitRentApiClient,
                resolutionSession,
                rentalTransactionNormalizer,
                rentalTransactionWriteService
        );
        verifyApiCalls(inOrder);
        for (RawRentalTransaction rawTransaction : rawTransactions) {
            inOrder.verify(resolutionSession).resolve(rawTransaction);
            inOrder.verify(rentalTransactionNormalizer).normalize(rawTransaction, null);
        }
        inOrder.verify(rentalTransactionWriteService).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                transactionCaptor.capture(),
                categoryCountsCaptor.capture()
        );
        inOrder.verify(resolutionSession).close();
        inOrder.verifyNoMoreInteractions();
        assertThat(transactionCaptor.getValue()).containsExactlyElementsOf(normalizedTransactions);
        assertCategoryCounts(categoryCountsCaptor.getValue(), 2, 1, 2);
    }

    @Test
    void 세_API가_모두_비어_있어도_빈_목록으로_한_번_저장한다() {
        stubEmptyApiResults();
        ArgumentCaptor<List<RentalTransaction>> transactionCaptor = transactionListCaptor();
        ArgumentCaptor<RentalTransactionCategoryCounts> categoryCountsCaptor = categoryCountsCaptor();

        ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);

        InOrder inOrder = inOrder(molitRentApiClient, rentalTransactionWriteService);
        verifyApiCalls(inOrder);
        inOrder.verify(rentalTransactionWriteService).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                transactionCaptor.capture(),
                categoryCountsCaptor.capture()
        );
        inOrder.verifyNoMoreInteractions();
        assertThat(transactionCaptor.getValue()).isEmpty();
        assertCategoryCounts(categoryCountsCaptor.getValue(), 0, 0, 0);
        verifyNoInteractions(rentalTransactionNormalizer);
    }

    @Test
    void 오피스텔_API만_비어_있으면_0_1_1_건수를_전달한다() {
        List<RawRentalTransaction> rawTransactions = List.of(
                createRawTransaction(MolitRentApiCategory.ROW_HOUSE, 1),
                createRawTransaction(MolitRentApiCategory.SINGLE_HOUSE, 2)
        );
        List<RentalTransaction> normalizedTransactions = List.of(
                createRentalTransaction(MolitRentApiCategory.ROW_HOUSE, 1),
                createRentalTransaction(MolitRentApiCategory.SINGLE_HOUSE, 2)
        );
        stubApiResults(rawTransactions);
        stubNormalizationResults(rawTransactions, normalizedTransactions);
        ArgumentCaptor<RentalTransactionCategoryCounts> categoryCountsCaptor = categoryCountsCaptor();

        ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);

        verify(rentalTransactionWriteService).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                eq(normalizedTransactions),
                categoryCountsCaptor.capture()
        );
        assertCategoryCounts(categoryCountsCaptor.getValue(), 0, 1, 1);
    }

    @Test
    void 연립_다세대_API만_비어_있으면_1_0_1_건수를_전달한다() {
        List<RawRentalTransaction> rawTransactions = List.of(
                createRawTransaction(MolitRentApiCategory.OFFICETEL, 1),
                createRawTransaction(MolitRentApiCategory.SINGLE_HOUSE, 2)
        );
        List<RentalTransaction> normalizedTransactions = List.of(
                createRentalTransaction(MolitRentApiCategory.OFFICETEL, 1),
                createRentalTransaction(MolitRentApiCategory.SINGLE_HOUSE, 2)
        );
        stubApiResults(rawTransactions);
        stubNormalizationResults(rawTransactions, normalizedTransactions);
        ArgumentCaptor<RentalTransactionCategoryCounts> categoryCountsCaptor = categoryCountsCaptor();

        ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);

        verify(rentalTransactionWriteService).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                eq(normalizedTransactions),
                categoryCountsCaptor.capture()
        );
        assertCategoryCounts(categoryCountsCaptor.getValue(), 1, 0, 1);
    }

    @Test
    void 단독_다가구_API만_비어_있으면_1_1_0_건수를_전달한다() {
        List<RawRentalTransaction> rawTransactions = List.of(
                createRawTransaction(MolitRentApiCategory.OFFICETEL, 1),
                createRawTransaction(MolitRentApiCategory.ROW_HOUSE, 2)
        );
        List<RentalTransaction> normalizedTransactions = List.of(
                createRentalTransaction(MolitRentApiCategory.OFFICETEL, 1),
                createRentalTransaction(MolitRentApiCategory.ROW_HOUSE, 2)
        );
        stubApiResults(rawTransactions);
        stubNormalizationResults(rawTransactions, normalizedTransactions);
        ArgumentCaptor<RentalTransactionCategoryCounts> categoryCountsCaptor = categoryCountsCaptor();

        ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);

        verify(rentalTransactionWriteService).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                eq(normalizedTransactions),
                categoryCountsCaptor.capture()
        );
        assertCategoryCounts(categoryCountsCaptor.getValue(), 1, 1, 0);
    }

    @Test
    void 첫_번째_API가_실패하면_즉시_중단하고_동일한_예외를_전파한다() {
        MolitRentApiException apiException = new MolitRentApiException("OFFICETEL 호출 실패");
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenThrow(apiException);

        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)
        );

        assertThat(actual).isSameAs(apiException);
        InOrder inOrder = inOrder(molitRentApiClient);
        verifyApiCall(inOrder, MolitRentApiCategory.OFFICETEL);
        inOrder.verifyNoMoreInteractions();
        verifyNoInteractions(rentalTransactionNormalizer, rentalTransactionWriteService);
    }

    @Test
    void 두_번째_API가_실패하면_세_번째_API를_호출하지_않고_동일한_예외를_전파한다() {
        RawRentalTransaction officetel = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        MolitRentApiException apiException = new MolitRentApiException("ROW_HOUSE 호출 실패");
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenReturn(List.of(officetel));
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.ROW_HOUSE,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenThrow(apiException);

        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)
        );

        assertThat(actual).isSameAs(apiException);
        InOrder inOrder = inOrder(molitRentApiClient);
        verifyApiCall(inOrder, MolitRentApiCategory.OFFICETEL);
        verifyApiCall(inOrder, MolitRentApiCategory.ROW_HOUSE);
        inOrder.verifyNoMoreInteractions();
        verifyNoInteractions(rentalTransactionNormalizer, rentalTransactionWriteService);
    }

    @Test
    void 세_번째_API가_실패하면_모든_API까지만_호출하고_동일한_예외를_전파한다() {
        RawRentalTransaction officetel = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        RawRentalTransaction rowHouse = createRawTransaction(MolitRentApiCategory.ROW_HOUSE, 2);
        MolitRentApiException apiException = new MolitRentApiException("SINGLE_HOUSE 호출 실패");
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenReturn(List.of(officetel));
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.ROW_HOUSE,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenReturn(List.of(rowHouse));
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.SINGLE_HOUSE,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenThrow(apiException);

        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)
        );

        assertThat(actual).isSameAs(apiException);
        InOrder inOrder = inOrder(molitRentApiClient);
        verifyApiCalls(inOrder);
        inOrder.verifyNoMoreInteractions();
        verifyNoInteractions(rentalTransactionNormalizer, rentalTransactionWriteService);
    }

    @Test
    void 첫_거래_정규화가_실패하면_수집만_완료하고_저장하지_않는다() {
        List<RawRentalTransaction> rawTransactions = createOneTransactionPerApi();
        InvalidMolitRentDataException normalizeException =
                new InvalidMolitRentDataException("첫 거래 정규화 실패");
        stubApiResults(rawTransactions);
        when(rentalTransactionNormalizer.normalize(rawTransactions.get(0), null))
                .thenThrow(normalizeException);

        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)
        );

        assertThat(actual).isSameAs(normalizeException);
        InOrder inOrder = inOrder(molitRentApiClient, resolutionSession, rentalTransactionNormalizer);
        verifyApiCalls(inOrder);
        inOrder.verify(resolutionSession).resolve(rawTransactions.get(0));
        inOrder.verify(rentalTransactionNormalizer).normalize(rawTransactions.get(0), null);
        inOrder.verify(resolutionSession).close();
        inOrder.verifyNoMoreInteractions();
        verifyNoInteractions(rentalTransactionWriteService);
    }

    @Test
    void 중간_거래_정규화가_실패하면_이전_거래까지만_정규화하고_저장하지_않는다() {
        RawRentalTransaction officetelFirst = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        RawRentalTransaction officetelSecond = createRawTransaction(MolitRentApiCategory.OFFICETEL, 2);
        RawRentalTransaction rowHouse = createRawTransaction(MolitRentApiCategory.ROW_HOUSE, 3);
        RawRentalTransaction singleHouse = createRawTransaction(MolitRentApiCategory.SINGLE_HOUSE, 4);
        List<RawRentalTransaction> rawTransactions = List.of(
                officetelFirst,
                officetelSecond,
                rowHouse,
                singleHouse
        );
        InvalidMolitRentDataException normalizeException =
                new InvalidMolitRentDataException("중간 거래 정규화 실패");
        stubApiResults(rawTransactions);
        when(rentalTransactionNormalizer.normalize(officetelFirst, null))
                .thenReturn(createRentalTransaction(MolitRentApiCategory.OFFICETEL, 1));
        when(rentalTransactionNormalizer.normalize(officetelSecond, null))
                .thenReturn(createRentalTransaction(MolitRentApiCategory.OFFICETEL, 2));
        when(rentalTransactionNormalizer.normalize(rowHouse, null)).thenThrow(normalizeException);

        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)
        );

        assertThat(actual).isSameAs(normalizeException);
        InOrder inOrder = inOrder(molitRentApiClient, resolutionSession, rentalTransactionNormalizer);
        verifyApiCalls(inOrder);
        inOrder.verify(resolutionSession).resolve(officetelFirst);
        inOrder.verify(rentalTransactionNormalizer).normalize(officetelFirst, null);
        inOrder.verify(resolutionSession).resolve(officetelSecond);
        inOrder.verify(rentalTransactionNormalizer).normalize(officetelSecond, null);
        inOrder.verify(resolutionSession).resolve(rowHouse);
        inOrder.verify(rentalTransactionNormalizer).normalize(rowHouse, null);
        inOrder.verify(resolutionSession).close();
        inOrder.verifyNoMoreInteractions();
        verifyNoInteractions(rentalTransactionWriteService);
    }

    @Test
    void resolver가_찾은_admin_dong_ID를_정규화와_저장에_전달한다() {
        RawRentalTransaction rawTransaction = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        RentalTransaction mappedTransaction = RentalTransaction.builder()
                .adminDongId(42L)
                .guCode(GU_CODE)
                .legalDongName("법정동-1")
                .rentalType("월세")
                .transactionDate(LocalDate.of(2026, 6, 1))
                .houseType("오피스텔")
                .area(new BigDecimal("40.1"))
                .depositAmount(10_001L)
                .monthlyRentAmount(1)
                .build();
        stubApiResults(List.of(rawTransaction));
        when(resolutionSession.resolve(rawTransaction)).thenReturn(RentalAdminDongResolution.mapped(42L));
        when(rentalTransactionNormalizer.normalize(rawTransaction, 42L)).thenReturn(mappedTransaction);

        ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);

        verify(rentalTransactionWriteService).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                eq(List.of(mappedTransaction)),
                org.mockito.ArgumentMatchers.any(RentalTransactionCategoryCounts.class)
        );
    }

    @Test
    void 카카오_API_장애가_발생하면_해당_구월을_저장하지_않는다() {
        RawRentalTransaction first = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        RawRentalTransaction second = createRawTransaction(MolitRentApiCategory.OFFICETEL, 2);
        KakaoRouteApiException failure = new KakaoRouteApiException("카카오 장애");
        stubApiResults(List.of(first, second));
        when(resolutionSession.resolve(first)).thenReturn(
                RentalAdminDongResolution.unmapped(RentalAdminDongResolution.Status.ADDRESS_NOT_FOUND)
        );
        when(resolutionSession.resolve(second)).thenThrow(failure);
        when(rentalTransactionNormalizer.normalize(first, null))
                .thenReturn(createRentalTransaction(MolitRentApiCategory.OFFICETEL, 1));

        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)
        );

        assertThat(actual).isSameAs(failure);
        verifyNoInteractions(rentalTransactionWriteService);
    }

    @Test
    void execution은_session을_공유하고_close는_멱등적이며_닫힌_뒤_수집만_거부한다() {
        RawRentalTransaction rawTransaction = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        RentalTransaction normalizedTransaction = createRentalTransaction(MolitRentApiCategory.OFFICETEL, 1);
        stubApiResults(List.of(rawTransaction));
        when(rentalTransactionNormalizer.normalize(rawTransaction, null)).thenReturn(normalizedTransaction);
        MolitRentIngestionService.IngestionExecution execution = ingestionService.openExecution();

        execution.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);
        execution.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);
        execution.close();
        execution.close();

        assertThat(catchThrowable(() -> execution.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)))
                .isExactlyInstanceOf(IllegalStateException.class);
        verify(rentalAdminDongResolver, times(1)).openSession();
        verify(resolutionSession, times(1)).close();
        verify(rentalTransactionWriteService, times(2)).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                eq(List.of(normalizedTransaction)),
                org.mockito.ArgumentMatchers.any(RentalTransactionCategoryCounts.class)
        );
    }

    @Test
    void session_생성_실패_후_같은_execution의_다음_구월_작업에서_재시도한다() {
        RawRentalTransaction rawTransaction = createRawTransaction(MolitRentApiCategory.OFFICETEL, 1);
        RentalTransaction normalizedTransaction = createRentalTransaction(MolitRentApiCategory.OFFICETEL, 1);
        IllegalStateException indexFailure = new IllegalStateException("행정동 인덱스 적재 실패");
        stubApiResults(List.of(rawTransaction));
        when(rentalAdminDongResolver.openSession())
                .thenThrow(indexFailure)
                .thenReturn(resolutionSession);
        when(rentalTransactionNormalizer.normalize(rawTransaction, null)).thenReturn(normalizedTransaction);
        MolitRentIngestionService.IngestionExecution execution = ingestionService.openExecution();

        assertThat(catchThrowable(
                () -> execution.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH)
        )).isSameAs(indexFailure);
        execution.ingestMonthlyTransactions(GU_CODE, DEAL_YEAR_MONTH);
        execution.close();

        verify(rentalAdminDongResolver, times(2)).openSession();
        verify(rentalTransactionWriteService, times(1)).replaceMonthlyTransactions(
                eq(GU_CODE),
                eq(DEAL_YEAR_MONTH),
                eq(List.of(normalizedTransaction)),
                org.mockito.ArgumentMatchers.any(RentalTransactionCategoryCounts.class)
        );
        verify(resolutionSession).close();
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidInputs")
    void 잘못된_입력은_어떤_의존성도_호출하지_않는다(
            String caseName,
            String guCode,
            YearMonth dealYearMonth
    ) {
        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions(guCode, dealYearMonth)
        );

        assertThat(actual).isExactlyInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(
                molitRentApiClient,
                rentalTransactionNormalizer,
                rentalTransactionWriteService,
                rentalAdminDongResolver,
                resolutionSession
        );
    }

    @Test
    void 지원하지_않는_정상_형식의_구_코드는_수집_시작_전에_거부한다() {
        Throwable actual = catchThrowable(
                () -> ingestionService.ingestMonthlyTransactions("26440", DEAL_YEAR_MONTH)
        );

        assertThat(actual)
                .isExactlyInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("지원하지 않는 서울");
        verifyNoInteractions(
                molitRentApiClient,
                rentalTransactionNormalizer,
                rentalTransactionWriteService,
                rentalAdminDongResolver,
                resolutionSession
        );
    }

    private void stubApiResults(List<RawRentalTransaction> transactions) {
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenReturn(filterByCategory(transactions, MolitRentApiCategory.OFFICETEL));
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.ROW_HOUSE,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenReturn(filterByCategory(transactions, MolitRentApiCategory.ROW_HOUSE));
        when(molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.SINGLE_HOUSE,
                GU_CODE,
                DEAL_YEAR_MONTH
        )).thenReturn(filterByCategory(transactions, MolitRentApiCategory.SINGLE_HOUSE));
    }

    private void stubEmptyApiResults() {
        stubApiResults(List.of());
    }

    private void stubNormalizationResults(
            List<RawRentalTransaction> rawTransactions,
            List<RentalTransaction> normalizedTransactions
    ) {
        for (int index = 0; index < rawTransactions.size(); index++) {
            when(rentalTransactionNormalizer.normalize(rawTransactions.get(index), null))
                    .thenReturn(normalizedTransactions.get(index));
        }
    }

    private List<RawRentalTransaction> filterByCategory(
            List<RawRentalTransaction> transactions,
            MolitRentApiCategory apiCategory
    ) {
        return transactions.stream()
                .filter(transaction -> transaction.getApiCategory() == apiCategory)
                .toList();
    }

    private List<RawRentalTransaction> createOneTransactionPerApi() {
        return List.of(
                createRawTransaction(MolitRentApiCategory.OFFICETEL, 1),
                createRawTransaction(MolitRentApiCategory.ROW_HOUSE, 2),
                createRawTransaction(MolitRentApiCategory.SINGLE_HOUSE, 3)
        );
    }

    private RawRentalTransaction createRawTransaction(MolitRentApiCategory apiCategory, int sequence) {
        return RawRentalTransaction.builder()
                .apiCategory(apiCategory)
                .guCode(GU_CODE)
                .legalDongName("법정동-" + sequence)
                .dealYear("2026")
                .dealMonth("6")
                .dealDay(String.valueOf(sequence))
                .houseType(rawHouseType(apiCategory))
                .exclusiveArea("40." + sequence)
                .totalFloorArea("50." + sequence)
                .deposit(String.valueOf(10_000 + sequence))
                .monthlyRent(String.valueOf(sequence))
                .build();
    }

    private RentalTransaction createRentalTransaction(MolitRentApiCategory apiCategory, int sequence) {
        return RentalTransaction.builder()
                .adminDongId(null)
                .guCode(GU_CODE)
                .legalDongName("법정동-" + sequence)
                .rentalType("월세")
                .transactionDate(LocalDate.of(2026, 6, sequence))
                .houseType(normalizedHouseType(apiCategory))
                .area(new BigDecimal("40." + sequence))
                .depositAmount(10_000L + sequence)
                .monthlyRentAmount(sequence)
                .build();
    }

    private String rawHouseType(MolitRentApiCategory apiCategory) {
        if (apiCategory == MolitRentApiCategory.ROW_HOUSE) {
            return "다세대";
        }
        if (apiCategory == MolitRentApiCategory.SINGLE_HOUSE) {
            return "다가구";
        }
        return null;
    }

    private String normalizedHouseType(MolitRentApiCategory apiCategory) {
        if (apiCategory == MolitRentApiCategory.OFFICETEL) {
            return "오피스텔";
        }
        return rawHouseType(apiCategory);
    }

    private void verifyApiCalls(InOrder inOrder) {
        verifyApiCall(inOrder, MolitRentApiCategory.OFFICETEL);
        verifyApiCall(inOrder, MolitRentApiCategory.ROW_HOUSE);
        verifyApiCall(inOrder, MolitRentApiCategory.SINGLE_HOUSE);
    }

    private void verifyApiCall(InOrder inOrder, MolitRentApiCategory apiCategory) {
        inOrder.verify(molitRentApiClient).fetchAllPages(apiCategory, GU_CODE, DEAL_YEAR_MONTH);
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<RentalTransaction>> transactionListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private ArgumentCaptor<RentalTransactionCategoryCounts> categoryCountsCaptor() {
        return ArgumentCaptor.forClass(RentalTransactionCategoryCounts.class);
    }

    private void assertCategoryCounts(
            RentalTransactionCategoryCounts counts,
            long officetelCount,
            long rowHouseCount,
            long singleHouseCount
    ) {
        assertThat(counts.getOfficetelCount()).isEqualTo(officetelCount);
        assertThat(counts.getRowHouseCount()).isEqualTo(rowHouseCount);
        assertThat(counts.getSingleHouseCount()).isEqualTo(singleHouseCount);
    }

    private static Stream<Arguments> invalidInputs() {
        return Stream.of(
                Arguments.of("guCode null", null, DEAL_YEAR_MONTH),
                Arguments.of("guCode 숫자 5자리 아님", "1162A", DEAL_YEAR_MONTH),
                Arguments.of("dealYearMonth null", GU_CODE, null)
        );
    }
}
