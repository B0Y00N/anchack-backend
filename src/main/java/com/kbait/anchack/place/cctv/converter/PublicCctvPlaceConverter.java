package com.kbait.anchack.place.cctv.converter;

import com.kbait.anchack.place.cctv.dto.PublicCctvRow;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;

import java.util.Objects;

public final class PublicCctvPlaceConverter {

    private static final long CCTV_DATA_SOURCE_ID = 6L;

    public ExternalPlace convert(PublicCctvRow row) {
        Objects.requireNonNull(row, "row must not be null.");

        return ExternalPlace.builder()
                .dataSourceId(CCTV_DATA_SOURCE_ID)
                .sourcePlaceId(row.getManagementNumber())
                .placeCategory(PlaceCategory.CCTV)
                .name(row.getManagementNumber())
                .address(row.getLotAddress())
                .roadAddress(row.getRoadAddress())
                .latitude(row.getLatitude())
                .longitude(row.getLongitude())
                .build();
    }
}
