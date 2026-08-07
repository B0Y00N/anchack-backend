package com.kbait.anchack.ingestion;

import com.kbait.anchack.ingestion.client.MolitRentApiCategory;
import com.kbait.anchack.ingestion.client.MolitRentApiClient;
import com.kbait.anchack.ingestion.config.MolitRentApiProperties;
import com.kbait.anchack.ingestion.domain.RentalTransaction;
import com.kbait.anchack.ingestion.exception.MolitRentApiResponseException;
import com.kbait.anchack.ingestion.mapper.RentalTransactionMapper;
import com.kbait.anchack.ingestion.normalizer.RentalTransactionNormalizer;
import com.kbait.anchack.ingestion.parser.MolitRentXmlParser;
import com.kbait.anchack.ingestion.service.MolitRentIngestionService;
import com.kbait.anchack.ingestion.service.MolitRentIngestionServiceImpl;
import com.kbait.anchack.ingestion.service.RentalTransactionWriteService;
import com.kbait.anchack.ingestion.service.RentalTransactionWriteServiceImpl;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@ExtendWith(MockitoExtension.class)
class MolitRentIngestionFlowTest {

    private static final String GU_CODE = "11620";
    private static final YearMonth DEAL_YEAR_MONTH = YearMonth.of(2026, 6);
    private static final LocalDate DATA_DATE = LocalDate.of(2026, 8, 5);
    private static final String FAKE_SERVICE_KEY = "MOLIT_FLOW_TEST_KEY+/=";
    private static final String ENCODED_FAKE_SERVICE_KEY = URLEncoder.encode(
            FAKE_SERVICE_KEY,
            StandardCharsets.UTF_8
    );
    private static final String DOUBLE_ENCODED_FAKE_SERVICE_KEY = URLEncoder.encode(
            ENCODED_FAKE_SERVICE_KEY,
            StandardCharsets.UTF_8
    );
    private static final int NUM_OF_ROWS = 100;
    private static final MediaType UTF8_XML = new MediaType(
            MediaType.APPLICATION_XML,
            StandardCharsets.UTF_8
    );
    private static final Pattern TOTAL_COUNT_PATTERN = Pattern.compile(
            "<totalCount>([0-9]+)</totalCount>"
    );
    private static final Set<String> ROW_HOUSE_TYPES = Set.of("연립", "다세대", "연립다세대");
    private static final Set<String> SINGLE_HOUSE_TYPES = Set.of("단독", "다가구");

    private static ConsoleCapture consoleCapture;

    @Mock
    private RentalTransactionMapper rentalTransactionMapper;

    private MockRestServiceServer server;
    private MolitRentIngestionService molitRentIngestionService;

    @BeforeAll
    static void startConsoleCapture() {
        consoleCapture = new ConsoleCapture();
    }

    @AfterAll
    static void verifySensitiveDataWasNotLogged() {
        consoleCapture.stop();

        assertThat(consoleCapture.getOutput()).doesNotContain(
                FAKE_SERVICE_KEY,
                ENCODED_FAKE_SERVICE_KEY,
                "serviceKey=",
                MolitRentApiCategory.OFFICETEL.getEndpoint(),
                MolitRentApiCategory.ROW_HOUSE.getEndpoint(),
                MolitRentApiCategory.SINGLE_HOUSE.getEndpoint()
        );
    }

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();

        MolitRentApiProperties properties = new MolitRentApiProperties(
                FAKE_SERVICE_KEY,
                NUM_OF_ROWS,
                1_000,
                1_000
        );
        MolitRentApiClient apiClient = new MolitRentApiClient(
                restTemplate,
                properties,
                new MolitRentXmlParser()
        );
        RentalTransactionWriteService writeService =
                new RentalTransactionWriteServiceImpl(rentalTransactionMapper);
        molitRentIngestionService = new MolitRentIngestionServiceImpl(
                apiClient,
                new RentalTransactionNormalizer(),
                writeService
        );
    }

    @Test
    void 실제_fixture_300건을_수집하고_정규화한_뒤_월별_거래를_교체한다() throws IOException {
        expectFixture(
                MolitRentApiCategory.OFFICETEL,
                "/molit/officetel_11620_202606.xml"
        );
        expectFixture(
                MolitRentApiCategory.ROW_HOUSE,
                "/molit/row_house_11620_202606.xml"
        );
        expectFixture(
                MolitRentApiCategory.SINGLE_HOUSE,
                "/molit/single_house_11620_202606.xml"
        );
        ArgumentCaptor<List<RentalTransaction>> transactionCaptor = transactionListCaptor();

        molitRentIngestionService.ingestMonthlyTransactions(
                GU_CODE,
                DEAL_YEAR_MONTH,
                DATA_DATE
        );

        server.verify();
        InOrder inOrder = inOrder(rentalTransactionMapper);
        inOrder.verify(rentalTransactionMapper).deleteByGuCodeAndTransactionDateRange(
                GU_CODE,
                LocalDate.of(2026, 6, 1),
                LocalDate.of(2026, 7, 1)
        );
        inOrder.verify(rentalTransactionMapper).insertBatch(transactionCaptor.capture());
        inOrder.verifyNoMoreInteractions();
        assertNormalizedTransactions(transactionCaptor.getValue());
    }

    @Test
    void 두_번째_API가_오류를_반환하면_DB_교체를_시작하지_않는다() throws IOException {
        expectFixture(
                MolitRentApiCategory.OFFICETEL,
                "/molit/officetel_11620_202606.xml"
        );
        server.expect(once(), expectedRequest(MolitRentApiCategory.ROW_HOUSE))
                .andRespond(withSuccess(apiErrorXml(), UTF8_XML));

        Throwable actual = catchThrowable(() -> molitRentIngestionService.ingestMonthlyTransactions(
                GU_CODE,
                DEAL_YEAR_MONTH,
                DATA_DATE
        ));

        assertThat(actual).isExactlyInstanceOf(MolitRentApiResponseException.class);
        MolitRentApiResponseException responseException = (MolitRentApiResponseException) actual;
        assertThat(responseException.getResultCode()).isEqualTo("30");
        assertThat(responseException.getResultMessage()).isEqualTo("SERVICE_KEY_IS_INVALID");
        server.verify();
        verifyNoInteractions(rentalTransactionMapper);
    }

    private void expectFixture(MolitRentApiCategory apiCategory, String resourcePath) throws IOException {
        server.expect(once(), expectedRequest(apiCategory))
                .andRespond(withSuccess(readAsSinglePage(resourcePath), UTF8_XML));
    }

    private RequestMatcher expectedRequest(MolitRentApiCategory apiCategory) {
        return request -> {
            URI actualUri = request.getURI();
            URI endpoint = URI.create(apiCategory.getEndpoint());
            String expectedQuery = "serviceKey=" + ENCODED_FAKE_SERVICE_KEY
                    + "&LAWD_CD=" + GU_CODE
                    + "&DEAL_YMD=202606"
                    + "&pageNo=1"
                    + "&numOfRows=" + NUM_OF_ROWS;

            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(actualUri.getScheme()).isEqualTo(endpoint.getScheme());
            assertThat(actualUri.getAuthority()).isEqualTo(endpoint.getAuthority());
            assertThat(actualUri.getPath()).isEqualTo(endpoint.getPath());
            assertThat(actualUri.getRawQuery()).isEqualTo(expectedQuery);
            assertThat(actualUri.getRawQuery()).containsOnlyOnce("serviceKey=");
            assertThat(actualUri.getRawQuery()).contains(ENCODED_FAKE_SERVICE_KEY);
            assertThat(actualUri.getRawQuery()).doesNotContain(DOUBLE_ENCODED_FAKE_SERVICE_KEY);
        };
    }

    private String readAsSinglePage(String resourcePath) throws IOException {
        String xml = readFixture(resourcePath);
        Matcher matcher = TOTAL_COUNT_PATTERN.matcher(xml);

        assertThat(matcher.find()).as(resourcePath + " totalCount 존재 여부").isTrue();
        assertThat(matcher.group(1)).as(resourcePath + " 원본 totalCount").isNotEqualTo("100");
        assertThat(matcher.find()).as(resourcePath + " totalCount 중복 여부").isFalse();

        return TOTAL_COUNT_PATTERN.matcher(xml).replaceFirst("<totalCount>100</totalCount>");
    }

    private String readFixture(String resourcePath) throws IOException {
        InputStream inputStream = Objects.requireNonNull(
                getClass().getResourceAsStream(resourcePath),
                resourcePath + " fixture를 찾을 수 없습니다."
        );

        try (inputStream) {
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private void assertNormalizedTransactions(List<RentalTransaction> transactions) {
        assertThat(transactions).hasSize(300);

        List<RentalTransaction> officetelTransactions = transactions.subList(0, 100);
        List<RentalTransaction> rowHouseTransactions = transactions.subList(100, 200);
        List<RentalTransaction> singleHouseTransactions = transactions.subList(200, 300);

        assertThat(officetelTransactions)
                .extracting(RentalTransaction::getHouseType)
                .containsOnly("오피스텔");
        assertThat(rowHouseTransactions)
                .extracting(RentalTransaction::getHouseType)
                .allMatch(ROW_HOUSE_TYPES::contains);
        assertThat(singleHouseTransactions)
                .extracting(RentalTransaction::getHouseType)
                .allMatch(SINGLE_HOUSE_TYPES::contains);

        assertCommonNormalizedFields(transactions);
        assertRoundedFixtureAreas(rowHouseTransactions, singleHouseTransactions);
        assertRepresentativeTransactions(transactions);
    }

    private void assertCommonNormalizedFields(List<RentalTransaction> transactions) {
        assertThat(transactions).allSatisfy(transaction -> {
            assertThat(transaction.getGuCode()).isEqualTo(GU_CODE);
            assertThat(transaction.getAdminDongId()).isNull();
            assertThat(transaction.getMaintenanceFee()).isZero();
            assertThat(transaction.getDataDate()).isEqualTo(DATA_DATE);
            assertThat(transaction.getArea().scale()).isEqualTo(2);
        });
    }

    private void assertRoundedFixtureAreas(
            List<RentalTransaction> rowHouseTransactions,
            List<RentalTransaction> singleHouseTransactions
    ) {
        assertThat(rowHouseTransactions)
                .extracting(RentalTransaction::getArea)
                .contains(new BigDecimal("11.83"), new BigDecimal("40.25"));
        assertThat(singleHouseTransactions)
                .extracting(RentalTransaction::getArea)
                .contains(new BigDecimal("26.63"));
    }

    private void assertRepresentativeTransactions(List<RentalTransaction> transactions) {
        RentalTransaction firstOfficetel = transactions.get(0);
        assertThat(firstOfficetel.getDeposit()).isEqualTo(4_000L).isNotEqualTo(40_000_000L);
        assertThat(firstOfficetel.getRent()).isEqualTo(64L).isNotEqualTo(640_000L);
        assertThat(firstOfficetel.getArea()).isEqualByComparingTo("16.34");

        RentalTransaction firstRowHouse = transactions.get(100);
        assertThat(firstRowHouse.getDeposit()).isEqualTo(22_422L).isNotEqualTo(224_220_000L);
        assertThat(firstRowHouse.getRent()).isEqualTo(31L).isNotEqualTo(310_000L);
        assertThat(firstRowHouse.getArea()).isEqualByComparingTo("45.53");

        RentalTransaction firstSingleHouse = transactions.get(200);
        assertThat(firstSingleHouse.getDeposit()).isEqualTo(12_500L).isNotEqualTo(125_000_000L);
        assertThat(firstSingleHouse.getRent()).isEqualTo(10L).isNotEqualTo(100_000L);
        assertThat(firstSingleHouse.getArea()).isEqualByComparingTo("20.00");
    }

    private String apiErrorXml() {
        return "<response>"
                + "<header>"
                + "<resultCode>30</resultCode>"
                + "<resultMsg>SERVICE_KEY_IS_INVALID</resultMsg>"
                + "</header>"
                + "</response>";
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<RentalTransaction>> transactionListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }

    private static final class ConsoleCapture {

        private final PrintStream originalOut = System.out;
        private final PrintStream originalError = System.err;
        private final ByteArrayOutputStream capturedOut = new ByteArrayOutputStream();
        private final ByteArrayOutputStream capturedError = new ByteArrayOutputStream();
        private final PrintStream out = new PrintStream(capturedOut, true, StandardCharsets.UTF_8);
        private final PrintStream error = new PrintStream(capturedError, true, StandardCharsets.UTF_8);

        private ConsoleCapture() {
            System.setOut(out);
            System.setErr(error);
        }

        private void stop() {
            out.flush();
            error.flush();
            System.setOut(originalOut);
            System.setErr(originalError);
            out.close();
            error.close();
        }

        private String getOutput() {
            return capturedOut.toString(StandardCharsets.UTF_8)
                    + capturedError.toString(StandardCharsets.UTF_8);
        }
    }
}
