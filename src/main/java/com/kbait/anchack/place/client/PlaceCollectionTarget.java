package com.kbait.anchack.place.client;

import com.kbait.anchack.place.domain.PlaceCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public final class PlaceCollectionTarget {

    private final Long dataSourceId;
    private final String guCode;
    private final String guName;
    private final KakaoPlaceSearchType searchType;
    private final String requestValue;
    private final PlaceCategory placeCategory;
    private final String expectedCategoryName;
    private final BigDecimal centerLatitude;
    private final BigDecimal centerLongitude;
}
