package com.kbait.anchack.rental.client;

import com.kbait.anchack.rental.config.MolitRentApiProperties;
import com.kbait.anchack.rental.dto.external.MolitRentPage;
import com.kbait.anchack.rental.dto.external.RawRentalTransaction;
import com.kbait.anchack.rental.exception.MolitRentApiException;
import com.kbait.anchack.rental.exception.MolitRentApiResponseException;
import com.kbait.anchack.rental.exception.MolitRentParseException;
import com.kbait.anchack.rental.parser.MolitRentXmlParser;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.client.RequestMatcher;
import org.springframework.web.client.RestTemplate;

import java.net.SocketTimeoutException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class MolitRentApiClientTest {

    private static final String FAKE_SERVICE_KEY = "MOLIT_TEST_KEY+/=";
    private static final String ENCODED_FAKE_SERVICE_KEY = URLEncoder.encode(
            FAKE_SERVICE_KEY,
            StandardCharsets.UTF_8
    );
    private static final String GU_CODE = "11110";
    private static final YearMonth DEAL_YEAR_MONTH = YearMonth.of(2024, 1);
    private static final int NUM_OF_ROWS = 100;

    private MockRestServiceServer server;
    private MolitRentApiClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = createClient(restTemplate, FAKE_SERVICE_KEY);
    }

    @ParameterizedTest
    @EnumSource(MolitRentApiCategory.class)
    void API_종류에_맞는_endpoint로_GET_요청한다(MolitRentApiCategory apiCategory) {
        expectPage(apiCategory, 1, pageXml(1, NUM_OF_ROWS, 0, 1, 0));

        client.fetchPage(apiCategory, GU_CODE, DEAL_YEAR_MONTH, 1);

        server.verify();
    }

    @Test
    void 서비스_키를_한_번만_인코딩하고_요청_파라미터를_정확히_전달한다() {
        String expectedQuery = "serviceKey=" + ENCODED_FAKE_SERVICE_KEY
                + "&LAWD_CD=11110&DEAL_YMD=202401&pageNo=2&numOfRows=100";
        String doubleEncodedServiceKey = URLEncoder.encode(ENCODED_FAKE_SERVICE_KEY, StandardCharsets.UTF_8);

        server.expect(once(), request -> {
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
                    assertThat(request.getURI().getRawQuery()).isEqualTo(expectedQuery);
                    assertThat(request.getURI().getRawQuery()).containsOnlyOnce("serviceKey=");
                    assertThat(request.getURI().getRawQuery()).contains(ENCODED_FAKE_SERVICE_KEY);
                    assertThat(request.getURI().getRawQuery()).doesNotContain(doubleEncodedServiceKey);
                })
                .andRespond(withSuccess(pageXml(2, NUM_OF_ROWS, 1, 1, 1), MediaType.APPLICATION_XML));

        client.fetchPage(MolitRentApiCategory.OFFICETEL, GU_CODE, DEAL_YEAR_MONTH, 2);

        server.verify();
    }

    @Test
    void 정상_XML을_MolitRentPage로_반환한다() {
        expectPage(
                MolitRentApiCategory.OFFICETEL,
                1,
                pageXml(1, NUM_OF_ROWS, 1, 7, 1)
        );

        MolitRentPage page = client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH,
                1
        );

        assertThat(page.getPageNo()).isEqualTo(1);
        assertThat(page.getNumOfRows()).isEqualTo(NUM_OF_ROWS);
        assertThat(page.getTotalCount()).isEqualTo(1);
        assertThat(page.getItems()).singleElement().satisfies(item -> {
            assertThat(item.getApiCategory()).isEqualTo(MolitRentApiCategory.OFFICETEL);
            assertThat(item.getDeposit()).isEqualTo("7");
        });
        server.verify();
    }

    @Test
    void 빈_서비스_키는_HTTP_요청_전에_거부한다() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer emptyKeyServer = MockRestServiceServer.bindTo(restTemplate).build();
        MolitRentApiClient emptyKeyClient = createClient(restTemplate, "");

        Throwable exception = catchThrowable(() -> emptyKeyClient.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH,
                1
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class);
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        emptyKeyServer.verify();
    }

    @Test
    void 숫자_다섯_자리가_아닌_구_코드는_HTTP_요청_전에_거부한다() {
        Throwable exception = catchThrowable(() -> client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                "1111A",
                DEAL_YEAR_MONTH,
                1
        ));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    void 일보다_작은_페이지_번호는_HTTP_요청_전에_거부한다() {
        Throwable exception = catchThrowable(() -> client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH,
                0
        ));

        assertThat(exception).isExactlyInstanceOf(IllegalArgumentException.class);
        server.verify();
    }

    @Test
    void API_종류가_null이면_HTTP_요청_전에_거부한다() {
        Throwable exception = catchThrowable(() -> client.fetchPage(null, GU_CODE, DEAL_YEAR_MONTH, 1));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        server.verify();
    }

    @Test
    void 계약연월이_null이면_HTTP_요청_전에_거부한다() {
        Throwable exception = catchThrowable(() -> client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                null,
                1
        ));

        assertThat(exception).isExactlyInstanceOf(NullPointerException.class);
        server.verify();
    }

    @Test
    void HTTP_오류_XML은_MolitRentApiResponseException으로_그대로_전파한다() {
        String errorXml = errorResponseXml("30", "SERVICE_KEY_IS_INVALID");
        server.expect(once(), expectedRequest(MolitRentApiCategory.OFFICETEL, 1))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_XML)
                        .body(errorXml));

        Throwable exception = catchThrowable(() -> client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH,
                1
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiResponseException.class);
        MolitRentApiResponseException responseException = (MolitRentApiResponseException) exception;
        assertThat(responseException.getResultCode()).isEqualTo("30");
        assertThat(responseException.getResultMessage()).isEqualTo("SERVICE_KEY_IS_INVALID");
        assertThat(responseException).hasNoCause();
        assertSensitiveDataIsAbsent(responseException, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void XML_API_오류가_아닌_HTTP_오류는_정제된_MolitRentApiException으로_변환한다() {
        server.expect(once(), expectedRequest(MolitRentApiCategory.OFFICETEL, 1))
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                        .contentType(MediaType.TEXT_PLAIN)
                        .body("internal error"));

        Throwable exception = catchThrowable(() -> client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH,
                1
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class);
        assertThat(exception).hasMessageContaining("httpStatus=500").hasNoCause();
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 네트워크_오류는_원본_URI를_제거한_MolitRentApiException으로_변환한다() {
        String unsafeCauseMessage = FAKE_SERVICE_KEY + " "
                + ENCODED_FAKE_SERVICE_KEY + " "
                + expectedUri(MolitRentApiCategory.OFFICETEL, 1);
        server.expect(once(), expectedRequest(MolitRentApiCategory.OFFICETEL, 1))
                .andRespond(withException(new SocketTimeoutException(unsafeCauseMessage)));

        Throwable exception = catchThrowable(() -> client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH,
                1
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class);
        assertThat(exception).hasNoCause();
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 정상_HTTP의_malformed_XML은_MolitRentParseException으로_전파한다() {
        server.expect(once(), expectedRequest(MolitRentApiCategory.OFFICETEL, 1))
                .andRespond(withSuccess("<response>", MediaType.APPLICATION_XML));

        Throwable exception = catchThrowable(() -> client.fetchPage(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH,
                1
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentParseException.class).hasCauseInstanceOf(Exception.class);
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 전체_건수가_영이면_첫_페이지만_호출하고_빈_목록을_반환한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 0, 1, 0));

        List<RawRentalTransaction> result = client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        );

        assertThat(result).isEmpty();
        server.verify();
    }

    @Test
    void 전체_건수가_페이지_크기와_같으면_첫_페이지만_호출한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 100, 1, 100));

        List<RawRentalTransaction> result = client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        );

        assertThat(result).hasSize(100);
        server.verify();
    }

    @Test
    void 전체_건수가_201이면_세_페이지를_순서대로_호출한다() {
        expectThreePages();

        List<RawRentalTransaction> result = client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        );

        assertThat(result).hasSize(201);
        server.verify();
    }

    @Test
    void 응답_페이지_번호가_요청_페이지와_다르면_실패한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(2, NUM_OF_ROWS, 1, 1, 1));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class)
                .hasMessageContaining("requestedPage=1")
                .hasMessageContaining("responsePageNo=2");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 후속_페이지의_numOfRows가_첫_페이지와_다르면_실패한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 201, 1, 100));
        expectPage(MolitRentApiCategory.OFFICETEL, 2, pageXml(2, 99, 201, 101, 99));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class)
                .hasMessageContaining("requestedPage=2")
                .hasMessageContaining("numOfRows=99")
                .hasMessageContaining("expectedNumOfRows=100");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 후속_페이지의_totalCount가_첫_페이지와_다르면_실패한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 201, 1, 100));
        expectPage(MolitRentApiCategory.OFFICETEL, 2, pageXml(2, NUM_OF_ROWS, 202, 101, 100));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class)
                .hasMessageContaining("requestedPage=2")
                .hasMessageContaining("totalCount=202")
                .hasMessageContaining("expectedTotalCount=201");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 마지막_페이지_전에_빈_페이지를_받으면_실패한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 201, 1, 100));
        expectPage(MolitRentApiCategory.OFFICETEL, 2, pageXml(2, NUM_OF_ROWS, 201, 101, 0));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class)
                .hasMessageContaining("requestedPage=2")
                .hasMessageContaining("itemCount=0");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 페이지의_item_수가_numOfRows를_초과하면_실패한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, 1, 2, 1, 2));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class)
                .hasMessageContaining("itemCount=2")
                .hasMessageContaining("numOfRows=1");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 누적_item_수가_totalCount를_초과하면_즉시_실패한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 101, 1, 100));
        expectPage(MolitRentApiCategory.OFFICETEL, 2, pageXml(2, NUM_OF_ROWS, 101, 101, 2));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class)
                .hasMessageContaining("accumulatedItemCount=102")
                .hasMessageContaining("totalCount=101");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 마지막_누적_item_수가_totalCount보다_적으면_실패한다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 101, 1, 100));
        expectPage(MolitRentApiCategory.OFFICETEL, 2, pageXml(2, NUM_OF_ROWS, 101, 101, 0));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiException.class)
                .hasMessageContaining("accumulatedItemCount=100")
                .hasMessageContaining("totalCount=101");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 후속_페이지_호출이_실패하면_예외를_전파하고_다음_페이지를_호출하지_않는다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 201, 1, 100));
        server.expect(once(), expectedRequest(MolitRentApiCategory.OFFICETEL, 2))
                .andRespond(withStatus(HttpStatus.BAD_REQUEST)
                        .contentType(MediaType.APPLICATION_XML)
                        .body(errorResponseXml("20", "SERVICE_ACCESS_DENIED")));

        Throwable exception = catchThrowable(() -> client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        ));

        assertThat(exception).isExactlyInstanceOf(MolitRentApiResponseException.class);
        assertThat(((MolitRentApiResponseException) exception).getResultCode()).isEqualTo("20");
        assertSensitiveDataIsAbsent(exception, MolitRentApiCategory.OFFICETEL);
        server.verify();
    }

    @Test
    void 전체_페이지_결과는_수정할_수_없는_목록이다() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 1, 1, 1));
        List<RawRentalTransaction> result = client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        );

        Throwable exception = catchThrowable(() -> result.add(result.get(0)));

        assertThat(exception).isExactlyInstanceOf(UnsupportedOperationException.class);
        server.verify();
    }

    @Test
    void 전체_페이지_결과는_페이지와_item의_기존_순서를_유지한다() {
        expectThreePages();

        List<RawRentalTransaction> result = client.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                GU_CODE,
                DEAL_YEAR_MONTH
        );

        assertThat(result)
                .extracting(RawRentalTransaction::getDeposit)
                .containsExactlyElementsOf(identifierStrings(1, 201));
        server.verify();
    }

    private MolitRentApiClient createClient(RestTemplate restTemplate, String serviceKey) {
        MolitRentApiProperties properties = new MolitRentApiProperties(
                serviceKey,
                NUM_OF_ROWS,
                1_000,
                1_000
        );
        return new MolitRentApiClient(restTemplate, properties, new MolitRentXmlParser());
    }

    private void expectThreePages() {
        expectPage(MolitRentApiCategory.OFFICETEL, 1, pageXml(1, NUM_OF_ROWS, 201, 1, 100));
        expectPage(MolitRentApiCategory.OFFICETEL, 2, pageXml(2, NUM_OF_ROWS, 201, 101, 100));
        expectPage(MolitRentApiCategory.OFFICETEL, 3, pageXml(3, NUM_OF_ROWS, 201, 201, 1));
    }

    private void expectPage(MolitRentApiCategory apiCategory, int requestedPage, String responseXml) {
        server.expect(once(), expectedRequest(apiCategory, requestedPage))
                .andRespond(withSuccess(responseXml, MediaType.APPLICATION_XML));
    }

    private RequestMatcher expectedRequest(MolitRentApiCategory apiCategory, int requestedPage) {
        return request -> {
            assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
            assertThat(request.getURI().toString()).isEqualTo(expectedUri(apiCategory, requestedPage));
        };
    }

    private String expectedUri(MolitRentApiCategory apiCategory, int pageNo) {
        return apiCategory.getEndpoint()
                + "?serviceKey=" + ENCODED_FAKE_SERVICE_KEY
                + "&LAWD_CD=" + GU_CODE
                + "&DEAL_YMD=202401"
                + "&pageNo=" + pageNo
                + "&numOfRows=" + NUM_OF_ROWS;
    }

    private String pageXml(
            int pageNo,
            int numOfRows,
            int totalCount,
            int firstIdentifier,
            int itemCount
    ) {
        StringBuilder items = new StringBuilder("<items>");
        for (int index = 0; index < itemCount; index++) {
            int identifier = firstIdentifier + index;
            items.append(itemXml(identifier));
        }
        items.append("</items>");

        return "<response>"
                + "<header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>"
                + "<body>"
                + items
                + "<numOfRows>" + numOfRows + "</numOfRows>"
                + "<pageNo>" + pageNo + "</pageNo>"
                + "<totalCount>" + totalCount + "</totalCount>"
                + "</body>"
                + "</response>";
    }

    private String itemXml(int identifier) {
        return "<item>"
                + "<sggCd>11110</sggCd>"
                + "<umdNm>청운동</umdNm>"
                + "<dealYear>2024</dealYear>"
                + "<dealMonth>1</dealMonth>"
                + "<dealDay>" + identifier + "</dealDay>"
                + "<excluUseAr>10.0</excluUseAr>"
                + "<deposit>" + identifier + "</deposit>"
                + "<monthlyRent>0</monthlyRent>"
                + "</item>";
    }

    private String errorResponseXml(String resultCode, String resultMessage) {
        return "<response>"
                + "<header>"
                + "<resultCode>" + resultCode + "</resultCode>"
                + "<resultMsg>" + resultMessage + "</resultMsg>"
                + "</header>"
                + "</response>";
    }

    private List<String> identifierStrings(int firstIdentifier, int lastIdentifier) {
        return java.util.stream.IntStream.rangeClosed(firstIdentifier, lastIdentifier)
                .mapToObj(String::valueOf)
                .toList();
    }

    private void assertSensitiveDataIsAbsent(Throwable exception, MolitRentApiCategory apiCategory) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null) {
                assertThat(current.getMessage()).doesNotContain(
                        FAKE_SERVICE_KEY,
                        ENCODED_FAKE_SERVICE_KEY,
                        "serviceKey=",
                        apiCategory.getEndpoint()
                );
            }
            current = current.getCause();
        }
    }
}
