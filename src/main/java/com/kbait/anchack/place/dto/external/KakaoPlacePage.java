package com.kbait.anchack.place.dto.external;

import lombok.Getter;

import java.util.List;

@Getter
public final class KakaoPlacePage {

    private final List<ExternalPlace> places;
    private final boolean end;

    public KakaoPlacePage(
            List<ExternalPlace> places,
            boolean end
    ) {
        this.places = List.copyOf(places);
        this.end = end;
    }
}
