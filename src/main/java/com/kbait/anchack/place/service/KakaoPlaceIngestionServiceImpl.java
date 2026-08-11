package com.kbait.anchack.place.service;

import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.exception.InvalidPlaceDataException;
import com.kbait.anchack.place.normalizer.PlaceNormalizer;
import com.kbait.anchack.place.resolver.PlaceAdminDongResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class KakaoPlaceIngestionServiceImpl implements KakaoPlaceIngestionService {

    private final KakaoPlaceCollectionService kakaoPlaceCollectionService;
    private final PlaceNormalizer placeNormalizer;
    private final PlaceAdminDongResolver placeAdminDongResolver;
    private final PlaceWriteService placeWriteService;

    @Override
    public KakaoPlaceIngestionResult ingest() {
        List<ExternalPlace> externalPlaces = kakaoPlaceCollectionService.collect();
        List<Place> places = normalizePlaces(externalPlaces);
        int affectedRowCount = placeWriteService.upsertPlaces(places);

        return new KakaoPlaceIngestionResult(
                externalPlaces.size(),
                places.size(),
                externalPlaces.size() - places.size(),
                affectedRowCount
        );
    }

    private List<Place> normalizePlaces(List<ExternalPlace> externalPlaces) {
        List<Place> places = new ArrayList<>(externalPlaces.size());
        for (ExternalPlace externalPlace : externalPlaces) {
            normalizePlace(externalPlace).ifPresent(places::add);
        }

        return places;
    }

    private Optional<Place> normalizePlace(ExternalPlace externalPlace) {
        try {
            Place place = placeNormalizer.normalize(externalPlace);
            return placeAdminDongResolver.resolveAdminDongId(externalPlace)
                    .map(adminDongId -> withAdminDongId(place, adminDongId));
        } catch (InvalidPlaceDataException exception) {
            return Optional.empty();
        }
    }

    private Place withAdminDongId(
            Place place,
            Long adminDongId
    ) {
        return Place.builder()
                .id(place.getId())
                .externalId(place.getExternalId())
                .adminDongId(adminDongId)
                .category(place.getCategory())
                .name(place.getName())
                .latitude(place.getLatitude())
                .longitude(place.getLongitude())
                .dataSourceId(place.getDataSourceId())
                .build();
    }
}
