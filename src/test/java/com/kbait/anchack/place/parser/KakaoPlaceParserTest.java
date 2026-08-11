package com.kbait.anchack.place.parser;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.place.client.KakaoPlaceSearchType;
import com.kbait.anchack.place.client.PlaceCollectionTarget;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.dto.external.KakaoPlacePage;
import com.kbait.anchack.place.exception.KakaoPlaceParseException;
import com.kbait.anchack.place.validator.KakaoPlaceCategoryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class KakaoPlaceParserTest {

    private KakaoPlaceParser parser;

    @BeforeEach
    void setUp() {
        parser = new KakaoPlaceParser(
                new ObjectMapper(),
                new KakaoPlaceCategoryValidator()
        );
    }

    @Test
    void 카카오_카테고리_응답을_공통_외부_장소로_변환한다() {
        PlaceCollectionTarget target = createPharmacyTarget();

        KakaoPlacePage actual = parser.parse(categoryResponse(), target);

        assertThat(actual.isEnd()).isTrue();
        assertThat(actual.getPlaces()).hasSize(1);

        ExternalPlace place = actual.getPlaces().get(0);
        assertThat(place.getDataSourceId()).isEqualTo(13L);
        assertThat(place.getSourcePlaceId()).isEqualTo("12345");
        assertThat(place.getPlaceCategory()).isEqualTo(PlaceCategory.PHARMACY);
        assertThat(place.getName()).isEqualTo("안착약국");
        assertThat(place.getRawCategoryCode()).isEqualTo("PM9");
        assertThat(place.getRawCategoryName()).isEqualTo("의료,건강 > 약국");
        assertThat(place.getAddress()).isEqualTo("서울 관악구 봉천동");
        assertThat(place.getRoadAddress()).isEqualTo("서울 관악구 관악로 1");
        assertThat(place.getLatitude()).isEqualTo("37.478154");
        assertThat(place.getLongitude()).isEqualTo("126.951484");
    }

    @Test
    void 키워드_검색에서_기대_카테고리명이_다른_장소는_제외한다() {
        PlaceCollectionTarget target = createRiverTarget();

        KakaoPlacePage actual = parser.parse(keywordResponse(), target);

        assertThat(actual.getPlaces()).hasSize(1);
        assertThat(actual.getPlaces().get(0).getName()).isEqualTo("성북천");
    }

    @Test
    void 빈_응답은_파싱_예외를_발생시킨다() {
        Throwable actual = catchThrowable(
                () -> parser.parse(" ", createPharmacyTarget())
        );

        assertThat(actual).isExactlyInstanceOf(KakaoPlaceParseException.class);
    }

    private PlaceCollectionTarget createPharmacyTarget() {
        return PlaceCollectionTarget.builder()
                .dataSourceId(13L)
                .guCode("11620")
                .guName("관악구")
                .searchType(KakaoPlaceSearchType.CATEGORY)
                .requestValue("PM9")
                .placeCategory(PlaceCategory.PHARMACY)
                .centerLatitude(new BigDecimal("37.478154"))
                .centerLongitude(new BigDecimal("126.951484"))
                .build();
    }

    private PlaceCollectionTarget createRiverTarget() {
        return PlaceCollectionTarget.builder()
                .dataSourceId(13L)
                .guCode("11620")
                .guName("관악구")
                .searchType(KakaoPlaceSearchType.KEYWORD)
                .requestValue("하천")
                .placeCategory(PlaceCategory.RIVER)
                .expectedCategoryName("여행 > 관광,명소 > 하천")
                .centerLatitude(new BigDecimal("37.478154"))
                .centerLongitude(new BigDecimal("126.951484"))
                .build();
    }

    private String categoryResponse() {
        return """
                {
                  "meta": {
                    "total_count": 1,
                    "pageable_count": 1,
                    "is_end": true
                  },
                  "documents": [
                    {
                      "id": "12345",
                      "place_name": "안착약국",
                      "category_group_code": "PM9",
                      "category_name": "의료,건강 > 약국",
                      "address_name": "서울 관악구 봉천동",
                      "road_address_name": "서울 관악구 관악로 1",
                      "x": "126.951484",
                      "y": "37.478154"
                    }
                  ]
                }
                """;
    }

    private String keywordResponse() {
        return """
                {
                  "meta": {
                    "total_count": 2,
                    "pageable_count": 2,
                    "is_end": true
                  },
                  "documents": [
                    {
                      "id": "25727324",
                      "place_name": "성북천",
                      "category_group_code": "",
                      "category_name": "여행 > 관광,명소 > 하천",
                      "address_name": "서울 동대문구 신설동",
                      "road_address_name": "",
                      "x": "127.026762402832",
                      "y": "37.5784297169049"
                    },
                    {
                      "id": "99999",
                      "place_name": "하천식당",
                      "category_group_code": "FD6",
                      "category_name": "음식점 > 한식",
                      "address_name": "서울 관악구 봉천동",
                      "road_address_name": "",
                      "x": "126.951484",
                      "y": "37.478154"
                    }
                  ]
                }
                """;
    }
}
