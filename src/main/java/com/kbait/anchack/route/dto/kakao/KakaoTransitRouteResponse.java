package com.kbait.anchack.route.dto.kakao;

import lombok.Getter;

import java.util.List;

@Getter
public final class KakaoTransitRouteResponse {

    private String status;
    private List<KakaoTransitRoute> routes = List.of();
}
