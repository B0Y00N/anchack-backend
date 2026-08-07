package com.kbait.anchack.place.dto.external;

import com.kbait.anchack.place.domain.PlaceCategory;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public final class ExternalPlace {

    private final Long dataSourceId;
    private final String sourcePlaceId;
    private final PlaceCategory placeCategory;
    private final String name;
    private final String rawCategoryCode;
    private final String rawCategoryName;
    private final String address;
    private final String roadAddress;
    private final String adminDongCode;
    private final String adminDongName;
    private final String latitude;
    private final String longitude;
    private final LocalDate dataDate;
}
