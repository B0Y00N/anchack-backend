package com.kbait.anchack.place.parser;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.place.client.PlaceCollectionTarget;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.dto.external.KakaoPlacePage;
import com.kbait.anchack.place.dto.kakao.KakaoPlaceDocument;
import com.kbait.anchack.place.dto.kakao.KakaoPlaceResponse;
import com.kbait.anchack.place.exception.KakaoPlaceParseException;
import com.kbait.anchack.place.validator.KakaoPlaceCategoryValidator;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KakaoPlaceParser {

    private final ObjectMapper objectMapper;
    private final KakaoPlaceCategoryValidator categoryValidator;

    public KakaoPlaceParser(
            ObjectMapper objectMapper,
            KakaoPlaceCategoryValidator categoryValidator
    ) {
        this.objectMapper = Objects.requireNonNull(objectMapper, "objectMapper는 null일 수 없습니다.");
        this.categoryValidator = Objects.requireNonNull(categoryValidator, "categoryValidator는 null일 수 없습니다.");
    }

    public KakaoPlacePage parse(
            String responseBody,
            PlaceCollectionTarget target
    ) {
        validateInputs(responseBody, target);

        KakaoPlaceResponse response = parseResponse(responseBody);
        validateResponse(response);

        return new KakaoPlacePage(
                toExternalPlaces(response.getDocuments(), target),
                response.getMeta().isEnd()
        );
    }

    private KakaoPlaceResponse parseResponse(String responseBody) {
        try {
            return objectMapper.readValue(responseBody, KakaoPlaceResponse.class);
        } catch (JsonProcessingException exception) {
            throw new KakaoPlaceParseException("카카오 장소 응답을 파싱할 수 없습니다.", exception);
        }
    }

    private List<ExternalPlace> toExternalPlaces(
            List<KakaoPlaceDocument> documents,
            PlaceCollectionTarget target
    ) {
        List<ExternalPlace> places = new ArrayList<>();

        for (KakaoPlaceDocument document : documents) {
            if (!categoryValidator.isAllowed(target, document)) {
                continue;
            }

            places.add(toExternalPlace(document, target));
        }

        return places;
    }

    private ExternalPlace toExternalPlace(
            KakaoPlaceDocument document,
            PlaceCollectionTarget target
    ) {
        return ExternalPlace.builder()
                .dataSourceId(target.getDataSourceId())
                .sourcePlaceId(document.getId())
                .placeCategory(target.getPlaceCategory())
                .name(document.getPlaceName())
                .rawCategoryCode(document.getCategoryGroupCode())
                .rawCategoryName(document.getCategoryName())
                .address(document.getAddressName())
                .roadAddress(document.getRoadAddressName())
                .latitude(document.getY())
                .longitude(document.getX())
                .dataDate(target.getDataDate())
                .build();
    }

    private void validateInputs(
            String responseBody,
            PlaceCollectionTarget target
    ) {
        if (responseBody == null || responseBody.isBlank()) {
            throw new KakaoPlaceParseException("카카오 장소 응답이 비어 있습니다.");
        }
        if (target == null) {
            throw new KakaoPlaceParseException("카카오 장소 수집 대상이 없습니다.");
        }
    }

    private void validateResponse(KakaoPlaceResponse response) {
        if (response == null) {
            throw new KakaoPlaceParseException("카카오 장소 응답을 읽을 수 없습니다.");
        }
        if (response.getMeta() == null) {
            throw new KakaoPlaceParseException("카카오 장소 응답에 meta가 없습니다.");
        }
        if (response.getDocuments() == null) {
            throw new KakaoPlaceParseException("카카오 장소 응답에 documents가 없습니다.");
        }
    }
}
