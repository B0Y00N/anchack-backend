package com.kbait.anchack.ingestion.parser;

import com.kbait.anchack.ingestion.client.MolitRentApiCategory;
import com.kbait.anchack.ingestion.dto.external.MolitRentPage;
import com.kbait.anchack.ingestion.dto.external.RawRentalTransaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

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
}
