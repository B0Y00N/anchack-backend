package com.kbait.anchack.place.dto.kakao;

import lombok.Getter;

import java.util.List;

@Getter
public final class KakaoPlaceResponse {

    private KakaoPlaceMeta meta;
    private List<KakaoPlaceDocument> documents = List.of();
}
