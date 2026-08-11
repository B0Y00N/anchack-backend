package com.kbait.anchack.place.normalizer;

import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.exception.InvalidPlaceDataException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class PlaceNormalizerTest {

    private final PlaceNormalizer normalizer = new PlaceNormalizer();

    @Test
    void 외부_장소를_DB_저장용_장소로_변환한다() {
        ExternalPlace externalPlace = validPlaceBuilder()
                .sourcePlaceId(" kakao-123 ")
                .name(" 안착 약국 ")
                .latitude("37.4781544")
                .longitude("126.9514835")
                .build();

        Place result = normalizer.normalize(externalPlace);

        assertThat(result.getId()).isNull();
        assertThat(result.getAdminDongId()).isNull();
        assertThat(result.getExternalId()).isEqualTo("kakao-123");
        assertThat(result.getCategory()).isEqualTo(PlaceCategory.PHARMACY);
        assertThat(result.getName()).isEqualTo("안착 약국");
        assertThat(result.getLatitude()).isEqualTo(new BigDecimal("37.478154"));
        assertThat(result.getLongitude()).isEqualTo(new BigDecimal("126.951484"));
        assertThat(result.getLatitude().scale()).isEqualTo(6);
        assertThat(result.getLongitude().scale()).isEqualTo(6);
        assertThat(result.getDataSourceId()).isEqualTo(13L);
    }

    @Test
    void 원본_ID가_없으면_정규화된_장소_정보로_결정적_ID를_생성한다() {
        ExternalPlace first = validPlaceBuilder()
                .sourcePlaceId(" ")
                .name(" 안착약국 ")
                .latitude("37.4781541")
                .longitude("126.9514838")
                .build();
        ExternalPlace second = validPlaceBuilder()
                .sourcePlaceId(null)
                .name("안착약국")
                .latitude("37.4781544")
                .longitude("126.9514842")
                .build();

        Place firstResult = normalizer.normalize(first);
        Place secondResult = normalizer.normalize(second);

        assertThat(firstResult.getExternalId()).isEqualTo(secondResult.getExternalId());
        assertThat(firstResult.getExternalId()).hasSize(64);
    }

    @Test
    void 생성_ID는_중복_기준을_구성하는_값이_달라지면_달라진다() {
        ExternalPlace pharmacy = validPlaceBuilder().sourcePlaceId(null).build();
        ExternalPlace cafe = validPlaceBuilder()
                .sourcePlaceId(null)
                .placeCategory(PlaceCategory.CAFE)
                .build();

        String pharmacyId = normalizer.normalize(pharmacy).getExternalId();
        String cafeId = normalizer.normalize(cafe).getExternalId();

        assertThat(pharmacyId).isNotEqualTo(cafeId);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void 장소명이_없으면_예외가_발생한다(String name) {
        InvalidPlaceDataException exception = catchInvalid(
                () -> normalizer.normalize(validPlaceBuilder().name(name).build())
        );

        assertThat(exception).hasMessageContaining("name");
    }

    @Test
    void 장소명이_200자를_초과하면_예외가_발생한다() {
        InvalidPlaceDataException exception = catchInvalid(
                () -> normalizer.normalize(validPlaceBuilder().name("가".repeat(201)).build())
        );

        assertThat(exception).hasMessageContaining("name").hasMessageContaining("200자");
    }

    @ParameterizedTest
    @MethodSource("invalidCoordinates")
    void 좌표가_없거나_숫자_형식이_아니거나_범위를_벗어나면_예외가_발생한다(
            String latitude,
            String longitude,
            String fieldName
    ) {
        InvalidPlaceDataException exception = catchInvalid(
                () -> normalizer.normalize(validPlaceBuilder()
                        .latitude(latitude)
                        .longitude(longitude)
                        .build())
        );

        assertThat(exception).hasMessageContaining(fieldName);
    }

    @Test
    void 위도와_경도는_지리적_경계값을_허용한다() {
        Place result = normalizer.normalize(validPlaceBuilder()
                .latitude("-90")
                .longitude("180")
                .build());

        assertThat(result.getLatitude()).isEqualTo(new BigDecimal("-90.000000"));
        assertThat(result.getLongitude()).isEqualTo(new BigDecimal("180.000000"));
    }

    @Test
    void 데이터_출처_ID가_null이거나_0_이하이면_예외가_발생한다() {
        InvalidPlaceDataException nullException = catchInvalid(
                () -> normalizer.normalize(validPlaceBuilder().dataSourceId(null).build())
        );
        InvalidPlaceDataException zeroException = catchInvalid(
                () -> normalizer.normalize(validPlaceBuilder().dataSourceId(0L).build())
        );

        assertThat(nullException).hasMessageContaining("dataSourceId");
        assertThat(zeroException).hasMessageContaining("dataSourceId");
    }

    @Test
    void 카테고리가_없으면_예외가_발생한다() {
        InvalidPlaceDataException categoryException = catchInvalid(
                () -> normalizer.normalize(validPlaceBuilder().placeCategory(null).build())
        );

        assertThat(categoryException).hasMessageContaining("placeCategory");
    }

    @Test
    void 원본_ID가_200자를_초과하면_예외가_발생한다() {
        InvalidPlaceDataException exception = catchInvalid(
                () -> normalizer.normalize(validPlaceBuilder().sourcePlaceId("a".repeat(201)).build())
        );

        assertThat(exception).hasMessageContaining("sourcePlaceId").hasMessageContaining("200자");
    }

    private ExternalPlace.ExternalPlaceBuilder validPlaceBuilder() {
        return ExternalPlace.builder()
                .dataSourceId(13L)
                .sourcePlaceId("kakao-123")
                .placeCategory(PlaceCategory.PHARMACY)
                .name("안착약국")
                .rawCategoryCode("PM9")
                .rawCategoryName("의료,건강 > 약국")
                .address("서울 관악구 봉천동")
                .roadAddress("서울 관악구 관악로 1")
                .latitude("37.478154")
                .longitude("126.951484");
    }

    private InvalidPlaceDataException catchInvalid(Runnable action) {
        Throwable exception = catchThrowable(action::run);

        assertThat(exception).isExactlyInstanceOf(InvalidPlaceDataException.class);
        return (InvalidPlaceDataException) exception;
    }

    private static Stream<Arguments> invalidCoordinates() {
        return Stream.of(
                Arguments.of(null, "126.951484", "latitude"),
                Arguments.of(" ", "126.951484", "latitude"),
                Arguments.of("abc", "126.951484", "latitude"),
                Arguments.of("90.000001", "126.951484", "latitude"),
                Arguments.of("37.478154", null, "longitude"),
                Arguments.of("37.478154", " ", "longitude"),
                Arguments.of("37.478154", "abc", "longitude"),
                Arguments.of("37.478154", "180.000001", "longitude")
        );
    }
}
