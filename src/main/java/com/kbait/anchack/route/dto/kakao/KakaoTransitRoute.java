package com.kbait.anchack.route.dto.kakao;

import lombok.Getter;

import java.util.List;

@Getter
public final class KakaoTransitRoute {

    private KakaoTransitRouteProperties properties;
    private List<KakaoTransitStep> steps;
}
