package com.kbait.anchack.route.dto.kakao;

import lombok.Getter;

import java.util.List;

@Getter
public final class KakaoTransitStepProperties {

    private String type;
    private Integer time;
    private List<KakaoTransitVehicle> vehicles;
    private List<KakaoTransitStop> stops;
}
