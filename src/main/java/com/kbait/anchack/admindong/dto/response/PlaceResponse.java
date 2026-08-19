package com.kbait.anchack.admindong.dto.response;

import com.kbait.anchack.place.domain.PlaceCategory;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** API_USER_CONDITIONS_REVISION_REQUEST.md P2(행정동별 장소 좌표 조회) 응답 1건. */
@Getter
@Builder
public class PlaceResponse {

    private Long placeId;
    private PlaceCategory category;
    private String name;
    private BigDecimal lat;
    private BigDecimal lng;
}
