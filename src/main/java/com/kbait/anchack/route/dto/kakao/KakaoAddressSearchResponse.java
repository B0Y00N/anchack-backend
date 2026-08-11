package com.kbait.anchack.route.dto.kakao;

import lombok.Getter;

import java.util.List;

@Getter
public final class KakaoAddressSearchResponse {

    private List<KakaoAddressDocument> documents = List.of();
}
