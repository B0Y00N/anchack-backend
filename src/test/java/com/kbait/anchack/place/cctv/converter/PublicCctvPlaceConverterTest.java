package com.kbait.anchack.place.cctv.converter;

import com.kbait.anchack.place.cctv.dto.PublicCctvRow;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PublicCctvPlaceConverterTest {

    private PublicCctvPlaceConverter converter;

    @BeforeEach
    void setUp() {
        converter = new PublicCctvPlaceConverter();
    }

    @Test
    void convertMapsCctvRowToExternalPlace() {
        ExternalPlace actual = converter.convert(cctvRow());

        assertThat(actual.getDataSourceId()).isEqualTo(6L);
        assertThat(actual.getSourcePlaceId()).isEqualTo("202630000000800463");
        assertThat(actual.getPlaceCategory()).isEqualTo(PlaceCategory.CCTV);
        assertThat(actual.getName()).isEqualTo("202630000000800463");
        assertThat(actual.getRawCategoryCode()).isNull();
        assertThat(actual.getRawCategoryName()).isNull();
        assertThat(actual.getAddress()).isEqualTo("서울특별시 종로구 삼청동 산2-1");
        assertThat(actual.getRoadAddress()).isEqualTo("서울특별시 종로구 북촌로 134-1");
        assertThat(actual.getAdminDongCode()).isNull();
        assertThat(actual.getAdminDongName()).isNull();
        assertThat(actual.getLatitude()).isEqualTo("37.58785");
        assertThat(actual.getLongitude()).isEqualTo("126.9843");
    }

    private PublicCctvRow cctvRow() {
        return PublicCctvRow.builder()
                .openLocalGovernmentCode("3000000")
                .managementNumber("202630000000800463")
                .managementAgencyName("서울특별시 종로구청")
                .roadAddress("서울특별시 종로구 북촌로 134-1")
                .lotAddress("서울특별시 종로구 삼청동 산2-1")
                .purposeType("생활방범")
                .cameraCount("2")
                .cameraPixel("200")
                .directionInfo("360도 전방면")
                .retentionDays("30")
                .installedYearMonth("202005")
                .agencyPhoneNumber("02-2148-3033")
                .latitude("37.58785")
                .longitude("126.9843")
                .dataBaseDate("2026-05-18")
                .dataUpdateType("")
                .dataUpdateTime("2026-05-19 22:58:23")
                .lastModifiedTime("2026-05-18 15:24:18")
                .build();
    }
}
