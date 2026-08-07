package com.kbait.anchack.place.validator;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.place.client.KakaoPlaceSearchType;
import com.kbait.anchack.place.client.PlaceCollectionTarget;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.kakao.KakaoPlaceDocument;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class KakaoPlaceCategoryValidatorTest {

    private final KakaoPlaceCategoryValidator validator = new KakaoPlaceCategoryValidator();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void 카테고리_검색은_요청한_카테고리_코드와_응답_코드가_같으면_허용한다() throws Exception {
        PlaceCollectionTarget target = createCategoryTarget();
        KakaoPlaceDocument document = readDocument("""
                {
                  "category_group_code": "PM9"
                }
                """);

        boolean actual = validator.isAllowed(target, document);

        assertThat(actual).isTrue();
    }

    @Test
    void 키워드_검색은_기대_카테고리명과_응답_카테고리명이_같으면_허용한다() throws Exception {
        PlaceCollectionTarget target = createKeywordTarget();
        KakaoPlaceDocument document = readDocument("""
                {
                  "category_name": "여행 > 관광,명소 > 하천"
                }
                """);

        boolean actual = validator.isAllowed(target, document);

        assertThat(actual).isTrue();
    }

    @Test
    void 키워드_검색은_기대_카테고리명과_다르면_제외한다() throws Exception {
        PlaceCollectionTarget target = createKeywordTarget();
        KakaoPlaceDocument document = readDocument("""
                {
                  "category_name": "음식점 > 한식"
                }
                """);

        boolean actual = validator.isAllowed(target, document);

        assertThat(actual).isFalse();
    }

    private KakaoPlaceDocument readDocument(String json) throws Exception {
        return objectMapper.readValue(json, KakaoPlaceDocument.class);
    }

    private PlaceCollectionTarget createCategoryTarget() {
        return PlaceCollectionTarget.builder()
                .dataSourceId(13L)
                .dataDate(LocalDate.of(2026, 8, 4))
                .guCode("11620")
                .guName("관악구")
                .searchType(KakaoPlaceSearchType.CATEGORY)
                .requestValue("PM9")
                .placeCategory(PlaceCategory.PHARMACY)
                .centerLatitude(new BigDecimal("37.478154"))
                .centerLongitude(new BigDecimal("126.951484"))
                .build();
    }

    private PlaceCollectionTarget createKeywordTarget() {
        return PlaceCollectionTarget.builder()
                .dataSourceId(13L)
                .dataDate(LocalDate.of(2026, 8, 4))
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
}
