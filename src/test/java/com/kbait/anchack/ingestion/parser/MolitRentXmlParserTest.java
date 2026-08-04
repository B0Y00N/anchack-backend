package com.kbait.anchack.ingestion.parser;

import com.kbait.anchack.ingestion.client.MolitRentApiCategory;
import com.kbait.anchack.ingestion.dto.external.MolitRentPage;
import com.kbait.anchack.ingestion.dto.external.RawRentalTransaction;
import com.kbait.anchack.ingestion.exception.MolitRentApiResponseException;
import com.kbait.anchack.ingestion.exception.MolitRentParseException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class MolitRentXmlParserTest {

    private static final String FIXTURE_ROOT = "/molit/";

    private final MolitRentXmlParser parser = new MolitRentXmlParser();

    @ParameterizedTest(name = "{0}")
    @MethodSource("fixtureCases")
    void 아홉_개의_XML_fixture를_정상_응답으로_파싱한다(
            String fixtureName,
            MolitRentApiCategory apiCategory,
            int totalCount
    ) throws IOException {
        MolitRentPage page = parser.parse(apiCategory, readFixture(fixtureName));

        assertThat(page.getItems())
                .hasSize(100)
                .allSatisfy(transaction -> assertThat(transaction.getApiCategory()).isEqualTo(apiCategory));
        assertThat(page.getPageNo()).isEqualTo(1);
        assertThat(page.getNumOfRows()).isEqualTo(100);
        assertThat(page.getTotalCount()).isEqualTo(totalCount);
    }

    @Test
    void 오피스텔_대표_거래의_원문_필드를_파싱한다() throws IOException {
        RawRentalTransaction transaction = parseFirstTransaction(
                "officetel_11620_202606.xml",
                MolitRentApiCategory.OFFICETEL
        );

        assertThat(transaction)
                .extracting(
                        RawRentalTransaction::getGuCode,
                        RawRentalTransaction::getLegalDongName,
                        RawRentalTransaction::getDealYear,
                        RawRentalTransaction::getDealMonth,
                        RawRentalTransaction::getDealDay,
                        RawRentalTransaction::getDeposit,
                        RawRentalTransaction::getMonthlyRent,
                        RawRentalTransaction::getExclusiveArea
                )
                .containsExactly("11620", "봉천동", "2026", "6", "29", "4,000", "64", "16.34");
        assertThat(transaction.getHouseType()).isNull();
        assertThat(transaction.getTotalFloorArea()).isNull();
    }

    @Test
    void 연립_다세대_대표_거래의_원문_필드를_파싱한다() throws IOException {
        RawRentalTransaction transaction = parseFirstTransaction(
                "row_house_11620_202606.xml",
                MolitRentApiCategory.ROW_HOUSE
        );

        assertThat(transaction)
                .extracting(
                        RawRentalTransaction::getGuCode,
                        RawRentalTransaction::getLegalDongName,
                        RawRentalTransaction::getDealYear,
                        RawRentalTransaction::getDealMonth,
                        RawRentalTransaction::getDealDay,
                        RawRentalTransaction::getDeposit,
                        RawRentalTransaction::getMonthlyRent,
                        RawRentalTransaction::getExclusiveArea,
                        RawRentalTransaction::getHouseType
                )
                .containsExactly("11620", "신림동", "2026", "6", "17", "22,422", "31", "45.53", "다세대");
        assertThat(transaction.getTotalFloorArea()).isNull();
    }

    @Test
    void 단독_다가구_대표_거래의_원문_필드를_파싱한다() throws IOException {
        RawRentalTransaction transaction = parseFirstTransaction(
                "single_house_11620_202606.xml",
                MolitRentApiCategory.SINGLE_HOUSE
        );

        assertThat(transaction)
                .extracting(
                        RawRentalTransaction::getGuCode,
                        RawRentalTransaction::getLegalDongName,
                        RawRentalTransaction::getDealYear,
                        RawRentalTransaction::getDealMonth,
                        RawRentalTransaction::getDealDay,
                        RawRentalTransaction::getDeposit,
                        RawRentalTransaction::getMonthlyRent,
                        RawRentalTransaction::getTotalFloorArea,
                        RawRentalTransaction::getHouseType
                )
                .containsExactly("11620", "신림동", "2026", "6", "16", "12,500", "10", "20", "다가구");
        assertThat(transaction.getExclusiveArea()).isNull();
    }

    @Test
    void 거래가_없는_정상_응답은_빈_목록을_반환한다() {
        String xml = """
                <response>
                    <header>
                        <resultCode>000</resultCode>
                        <resultMsg>OK</resultMsg>
                    </header>
                    <body>
                        <items/>
                        <numOfRows>100</numOfRows>
                        <pageNo>1</pageNo>
                        <totalCount>0</totalCount>
                    </body>
                </response>
                """;

        MolitRentPage page = parser.parse(MolitRentApiCategory.OFFICETEL, xml);

        assertThat(page.getItems()).isEmpty();
    }

    @Test
    void response_오류는_body보다_먼저_API_응답_예외로_처리한다() {
        String xml = """
                <response>
                    <header>
                        <resultCode>99</resultCode>
                        <resultMsg>INVALID_REQUEST</resultMsg>
                    </header>
                </response>
                """;

        MolitRentApiResponseException exception = assertApiResponseException(xml);

        assertThat(exception.getResultCode()).isEqualTo("99");
        assertThat(exception.getResultMessage()).isEqualTo("INVALID_REQUEST");
        assertThat(exception.getMessage()).contains("99", "INVALID_REQUEST");
    }

    @Test
    void 공공데이터포털_공통_오류는_API_응답_예외로_처리한다() {
        String xml = """
                <OpenAPI_ServiceResponse>
                    <cmmMsgHeader>
                        <errMsg>SERVICE_KEY_IS_NULL</errMsg>
                        <returnAuthMsg>서비스 접근거부</returnAuthMsg>
                        <returnReasonCode>20</returnReasonCode>
                    </cmmMsgHeader>
                </OpenAPI_ServiceResponse>
                """;

        MolitRentApiResponseException exception = assertApiResponseException(xml);

        assertThat(exception.getResultCode()).isEqualTo("20");
        assertThat(exception.getResultMessage()).isEqualTo("서비스 접근거부");
    }

    @Test
    void 공통_오류에_returnAuthMsg가_없으면_errMsg를_사용한다() {
        String xml = """
                <OpenAPI_ServiceResponse>
                    <cmmMsgHeader>
                        <errMsg>SERVICE_KEY_IS_NULL</errMsg>
                        <returnReasonCode>20</returnReasonCode>
                    </cmmMsgHeader>
                </OpenAPI_ServiceResponse>
                """;

        MolitRentApiResponseException exception = assertApiResponseException(xml);

        assertThat(exception.getResultCode()).isEqualTo("20");
        assertThat(exception.getResultMessage()).isEqualTo("SERVICE_KEY_IS_NULL");
    }

    @Test
    void null_XML은_파싱_예외로_처리한다() {
        MolitRentParseException exception = assertParseException(null);

        assertThat(exception.getMessage()).contains("비어");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    void 빈_XML은_파싱_예외로_처리한다(String xml) {
        MolitRentParseException exception = assertParseException(xml);

        assertThat(exception.getMessage()).contains("비어");
    }

    @Test
    void malformed_XML은_SAXException을_원인으로_보존한다() {
        MolitRentParseException exception = assertParseException("<response><header></response>");

        assertThat(exception).hasCauseInstanceOf(SAXException.class);
    }

    @Test
    void DOCTYPE이_포함된_XML은_SAXException을_원인으로_보존한다() {
        String xml = """
                <!DOCTYPE response [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
                <response>
                    <header>
                        <resultCode>000</resultCode>
                        <resultMsg>OK</resultMsg>
                    </header>
                    <body>
                        <items/>
                        <pageNo>1</pageNo>
                        <numOfRows>100</numOfRows>
                        <totalCount>0</totalCount>
                    </body>
                </response>
                """;

        MolitRentParseException exception = assertParseException(xml);

        assertThat(exception).hasCauseInstanceOf(SAXException.class);
    }

    @Test
    void 지원하지_않는_최상위_요소는_파싱_예외로_처리한다() {
        MolitRentParseException exception = assertParseException("<unsupported/>");

        assertThat(exception.getMessage()).contains("최상위");
    }

    @Test
    void header가_누락되면_파싱_예외로_처리한다() {
        String xml = "<response>" + pageBody("1", "100", "0") + "</response>";

        MolitRentParseException exception = assertParseException(xml);

        assertThat(exception.getMessage()).contains("header");
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("invalidResultCodeCases")
    void resultCode가_누락되거나_비어_있으면_파싱_예외로_처리한다(String caseName, String xml) {
        MolitRentParseException exception = assertParseException(xml);

        assertThat(exception.getMessage()).contains("resultCode");
    }

    @Test
    void 정상_응답의_body가_누락되면_파싱_예외로_처리한다() {
        MolitRentParseException exception = assertParseException(successResponse(""));

        assertThat(exception.getMessage()).contains("body");
    }

    @Test
    void 정상_응답의_items가_누락되면_파싱_예외로_처리한다() {
        String body = "<body><pageNo>1</pageNo><numOfRows>100</numOfRows><totalCount>0</totalCount></body>";

        MolitRentParseException exception = assertParseException(successResponse(body));

        assertThat(exception.getMessage()).contains("items");
    }

    @ParameterizedTest(name = "{0} 누락")
    @MethodSource("missingPageValueCases")
    void 필수_페이지_요소가_누락되면_파싱_예외로_처리한다(String tagName, String xml) {
        MolitRentParseException exception = assertParseException(xml);

        assertThat(exception.getMessage()).contains(tagName);
    }

    @ParameterizedTest(name = "{0} 빈 값")
    @MethodSource("blankPageValueCases")
    void 페이지_값이_비어_있으면_파싱_예외로_처리한다(String tagName, String xml) {
        MolitRentParseException exception = assertParseException(xml);

        assertThat(exception.getMessage()).contains(tagName, "비어");
    }

    @ParameterizedTest(name = "{0} 비정수")
    @MethodSource("invalidPageValueCases")
    void 페이지_값이_정수가_아니면_숫자_형식_예외를_원인으로_보존한다(
            String tagName,
            String xml
    ) {
        MolitRentParseException exception = assertParseException(xml);

        assertThat(exception.getMessage()).contains(tagName);
        assertThat(exception).hasCauseInstanceOf(NumberFormatException.class);
    }

    private MolitRentApiResponseException assertApiResponseException(String xml) {
        Throwable thrown = catchThrowable(() -> parser.parse(MolitRentApiCategory.OFFICETEL, xml));

        assertThat(thrown)
                .isExactlyInstanceOf(MolitRentApiResponseException.class)
                .isNotInstanceOf(MolitRentParseException.class);
        return (MolitRentApiResponseException) thrown;
    }

    private MolitRentParseException assertParseException(String xml) {
        Throwable thrown = catchThrowable(() -> parser.parse(MolitRentApiCategory.OFFICETEL, xml));

        assertThat(thrown).isExactlyInstanceOf(MolitRentParseException.class);
        return (MolitRentParseException) thrown;
    }

    private RawRentalTransaction parseFirstTransaction(
            String fixtureName,
            MolitRentApiCategory apiCategory
    ) throws IOException {
        MolitRentPage page = parser.parse(apiCategory, readFixture(fixtureName));
        return page.getItems().get(0);
    }

    private String readFixture(String fixtureName) throws IOException {
        String resourcePath = FIXTURE_ROOT + fixtureName;

        try (InputStream inputStream = MolitRentXmlParserTest.class.getResourceAsStream(resourcePath)) {
            assertThat(inputStream)
                    .as("classpath fixture: %s", resourcePath)
                    .isNotNull();
            return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static Stream<Arguments> fixtureCases() {
        return Stream.of(
                Arguments.of("officetel_11620_202407.xml", MolitRentApiCategory.OFFICETEL, 341),
                Arguments.of("officetel_11620_202507.xml", MolitRentApiCategory.OFFICETEL, 339),
                Arguments.of("officetel_11620_202606.xml", MolitRentApiCategory.OFFICETEL, 295),
                Arguments.of("row_house_11620_202407.xml", MolitRentApiCategory.ROW_HOUSE, 501),
                Arguments.of("row_house_11620_202507.xml", MolitRentApiCategory.ROW_HOUSE, 415),
                Arguments.of("row_house_11620_202606.xml", MolitRentApiCategory.ROW_HOUSE, 450),
                Arguments.of("single_house_11620_202407.xml", MolitRentApiCategory.SINGLE_HOUSE, 1620),
                Arguments.of("single_house_11620_202507.xml", MolitRentApiCategory.SINGLE_HOUSE, 1456),
                Arguments.of("single_house_11620_202606.xml", MolitRentApiCategory.SINGLE_HOUSE, 1237)
        );
    }

    private static Stream<Arguments> invalidResultCodeCases() {
        return Stream.of(
                Arguments.of("resultCode 누락", "<response><header><resultMsg>OK</resultMsg></header></response>"),
                Arguments.of(
                        "resultCode 빈 값",
                        "<response><header><resultCode> </resultCode><resultMsg>OK</resultMsg></header></response>"
                )
        );
    }

    private static Stream<Arguments> missingPageValueCases() {
        return Stream.of(
                Arguments.of("pageNo", successResponse(pageBody(null, "100", "0"))),
                Arguments.of("numOfRows", successResponse(pageBody("1", null, "0"))),
                Arguments.of("totalCount", successResponse(pageBody("1", "100", null)))
        );
    }

    private static Stream<Arguments> blankPageValueCases() {
        return Stream.of(
                Arguments.of("pageNo", successResponse(pageBody("", "100", "0"))),
                Arguments.of("numOfRows", successResponse(pageBody("1", "", "0"))),
                Arguments.of("totalCount", successResponse(pageBody("1", "100", "")))
        );
    }

    private static Stream<Arguments> invalidPageValueCases() {
        return Stream.of(
                Arguments.of("pageNo", successResponse(pageBody("one", "100", "0"))),
                Arguments.of("numOfRows", successResponse(pageBody("1", "hundred", "0"))),
                Arguments.of("totalCount", successResponse(pageBody("1", "100", "none")))
        );
    }

    private static String successResponse(String body) {
        return "<response><header><resultCode>000</resultCode><resultMsg>OK</resultMsg></header>"
                + body
                + "</response>";
    }

    private static String pageBody(String pageNo, String numOfRows, String totalCount) {
        return "<body><items/>"
                + pageElement("pageNo", pageNo)
                + pageElement("numOfRows", numOfRows)
                + pageElement("totalCount", totalCount)
                + "</body>";
    }

    private static String pageElement(String tagName, String value) {
        return value == null ? "" : "<" + tagName + ">" + value + "</" + tagName + ">";
    }
}
