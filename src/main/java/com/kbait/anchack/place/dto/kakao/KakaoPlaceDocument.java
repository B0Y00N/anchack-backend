package com.kbait.anchack.place.dto.kakao;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;

@Getter
public final class KakaoPlaceDocument {

    private String id;

    @JsonProperty("place_name")
    private String placeName;

    @JsonProperty("category_group_code")
    private String categoryGroupCode;

    @JsonProperty("category_name")
    private String categoryName;

    @JsonProperty("address_name")
    private String addressName;

    @JsonProperty("road_address_name")
    private String roadAddressName;

    private String x; //경도
    private String y; //위도
}
