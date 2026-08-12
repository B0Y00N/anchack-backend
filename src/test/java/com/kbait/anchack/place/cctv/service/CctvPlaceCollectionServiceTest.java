package com.kbait.anchack.place.cctv.service;

import com.kbait.anchack.place.cctv.converter.PublicCctvPlaceConverter;
import com.kbait.anchack.place.cctv.parser.PublicCctvCsvParser;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class CctvPlaceCollectionServiceTest {

    private CctvPlaceCollectionService collectionService;

    @BeforeEach
    void setUp() {
        collectionService = new CctvPlaceCollectionServiceImpl(
                new PublicCctvCsvParser(),
                new PublicCctvPlaceConverter()
        );
    }

    @Test
    void collectParsesCctvCsvAndConvertsRowsToExternalPlaces() {
        List<ExternalPlace> actual = collectionService.collect(new StringReader(validCsv()));

        assertThat(actual).hasSize(2);
        assertThat(actual)
                .extracting(ExternalPlace::getSourcePlaceId)
                .containsExactly("202630000000800463", "202630000000800464");
        assertThat(actual)
                .extracting(ExternalPlace::getDataSourceId)
                .containsOnly(6L);
        assertThat(actual)
                .extracting(ExternalPlace::getPlaceCategory)
                .containsOnly(PlaceCategory.CCTV);
    }

    @Test
    void collectReturnsUnmodifiableList() {
        List<ExternalPlace> actual = collectionService.collect(new StringReader(validCsv()));

        Throwable throwable = catchThrowable(() -> actual.add(actual.get(0)));

        assertThat(throwable).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    private String validCsv() {
        return """
                개방자치단체코드,관리번호,관리기관명,소재지도로명주소,소재지지번주소,설치목적구분,카메라대수,카메라화소수,촬영방면정보,보관일수,설치연월,관리기관전화번호,WGS84위도,WGS84경도,데이터기준일자,데이터갱신구분,데이터갱신시점,최종수정시점
                3000000,202630000000800463,서울특별시 종로구청,서울특별시 종로구 북촌로 134-1,,생활방범,2,200,360도 전방면,30,202005,02-2148-3033,37.58785,126.9843,2026-05-18,,2026-05-19 22:58:23,2026-05-18 15:24:18
                3000000,202630000000800464,서울특별시 종로구청,서울특별시 종로구 북촌로 135-1,,생활방범,1,200,고정형,30,202006,02-2148-3033,37.58800,126.9850,2026-05-18,,2026-05-19 22:58:23,2026-05-18 15:24:18
                """;
    }
}
