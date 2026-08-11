package com.kbait.anchack.place.service;

import com.kbait.anchack.place.client.KakaoPlaceApiClient;
import com.kbait.anchack.place.client.KakaoPlaceCollectionTargetProvider;
import com.kbait.anchack.place.client.PlaceCollectionTarget;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KakaoPlaceCollectionServiceImpl implements KakaoPlaceCollectionService {

    private final KakaoPlaceCollectionTargetProvider targetProvider;
    private final KakaoPlaceApiClient kakaoPlaceApiClient;

    @Override
    public List<ExternalPlace> collect() {
        List<PlaceCollectionTarget> targets = targetProvider.createTargets();
        List<ExternalPlace> places = new ArrayList<>();
        for (PlaceCollectionTarget target : targets) {
            places.addAll(kakaoPlaceApiClient.fetchPlaces(target));
        }

        return List.copyOf(places);
    }
}
