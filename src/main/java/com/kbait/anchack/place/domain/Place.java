package com.kbait.anchack.place.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public final class Place {

    private final Long id;
    private final String externalId;
    private final Long adminDongId;
    private final PlaceCategory category;
    private final String name;
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final Long dataSourceId;
}
