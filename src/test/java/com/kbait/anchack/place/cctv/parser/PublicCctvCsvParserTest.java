package com.kbait.anchack.place.cctv.parser;

import com.kbait.anchack.place.cctv.dto.PublicCctvRow;
import com.kbait.anchack.place.cctv.exception.PublicCctvCsvParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicCctvCsvParserTest {

    private PublicCctvCsvParser parser;

    @BeforeEach
    void setUp() {
        parser = new PublicCctvCsvParser();
    }

    @Test
    void parseMapsCctvCsvRecordToRow() {
        List<PublicCctvRow> actual = parser.parse(new StringReader(validCsv()));

        assertThat(actual).hasSize(1);

        PublicCctvRow row = actual.get(0);
        assertThat(row.getOpenLocalGovernmentCode()).isEqualTo("3000000");
        assertThat(row.getManagementNumber()).isEqualTo("202630000000800463");
        assertThat(row.getManagementAgencyName()).isEqualTo("서울특별시 종로구청");
        assertThat(row.getRoadAddress()).isEqualTo("서울특별시 종로구, 북촌로 134-1");
        assertThat(row.getLotAddress()).isEmpty();
        assertThat(row.getLatitude()).isEqualTo("37.58785");
        assertThat(row.getLongitude()).isEqualTo("126.9843");
        assertThat(row.getLastModifiedTime()).isEqualTo("2026-05-18 15:24:18");
    }

    @Test
    void parseThrowsExceptionWhenRequiredHeaderIsMissing() {
        String csv = "관리번호,WGS84위도,WGS84경도\n202630000000800463,37.58785,126.9843";

        assertThatThrownBy(() -> parser.parse(new StringReader(csv)))
                .isInstanceOf(PublicCctvCsvParseException.class)
                .hasMessageContaining("개방자치단체코드");
    }

    @Test
    void parseThrowsExceptionWhenRecordHasInvalidColumnCount() {
        String csv = validCsv().replace(",2026-05-18 15:24:18", "");

        assertThatThrownBy(() -> parser.parse(new StringReader(csv)))
                .isInstanceOf(PublicCctvCsvParseException.class)
                .hasMessageContaining("invalid column count");
    }

    private String validCsv() {
        return """
                개방자치단체코드,관리번호,관리기관명,소재지도로명주소,소재지지번주소,설치목적구분,카메라대수,카메라화소수,촬영방면정보,보관일수,설치연월,관리기관전화번호,WGS84위도,WGS84경도,데이터기준일자,데이터갱신구분,데이터갱신시점,최종수정시점
                3000000,202630000000800463,서울특별시 종로구청,"서울특별시 종로구, 북촌로 134-1",,생활방범,2,200,360도 전방면,30,202005,02-2148-3033,37.58785,126.9843,2026-05-18,,2026-05-19 22:58:23,2026-05-18 15:24:18
                """;
    }
}
