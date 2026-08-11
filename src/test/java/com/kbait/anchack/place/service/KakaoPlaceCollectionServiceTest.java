package com.kbait.anchack.place.service;

import com.kbait.anchack.place.client.KakaoPlaceApiClient;
import com.kbait.anchack.place.client.KakaoPlaceCollectionTargetProvider;
import com.kbait.anchack.place.client.KakaoPlaceSearchType;
import com.kbait.anchack.place.client.PlaceCollectionTarget;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.exception.KakaoPlaceApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoPlaceCollectionServiceTest {

    @Mock
    private KakaoPlaceCollectionTargetProvider targetProvider;

    @Mock
    private KakaoPlaceApiClient kakaoPlaceApiClient;

    private KakaoPlaceCollectionService collectionService;

    @BeforeEach
    void setUp() {
        collectionService = new KakaoPlaceCollectionServiceImpl(
                targetProvider,
                kakaoPlaceApiClient
        );
    }

    @Test
    void collectsPlacesForEveryTargetInOrder() {
        PlaceCollectionTarget pharmacyTarget = createTarget("11210", PlaceCategory.PHARMACY, "PM9");
        PlaceCollectionTarget cafeTarget = createTarget("11230", PlaceCategory.CAFE, "CE7");
        ExternalPlace pharmacy = createPlace("pharmacy-1", PlaceCategory.PHARMACY);
        ExternalPlace cafe = createPlace("cafe-1", PlaceCategory.CAFE);
        when(targetProvider.createTargets()).thenReturn(List.of(pharmacyTarget, cafeTarget));
        when(kakaoPlaceApiClient.fetchPlaces(pharmacyTarget)).thenReturn(List.of(pharmacy));
        when(kakaoPlaceApiClient.fetchPlaces(cafeTarget)).thenReturn(List.of(cafe));

        List<ExternalPlace> actual = collectionService.collect();

        assertThat(actual).containsExactly(pharmacy, cafe);
        InOrder inOrder = inOrder(targetProvider, kakaoPlaceApiClient);
        inOrder.verify(targetProvider).createTargets();
        inOrder.verify(kakaoPlaceApiClient).fetchPlaces(pharmacyTarget);
        inOrder.verify(kakaoPlaceApiClient).fetchPlaces(cafeTarget);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void returnsEmptyListWhenThereIsNoTarget() {
        when(targetProvider.createTargets()).thenReturn(List.of());

        List<ExternalPlace> actual = collectionService.collect();

        assertThat(actual).isEmpty();
        verifyNoInteractions(kakaoPlaceApiClient);
    }

    @Test
    void returnedPlacesCannotBeModified() {
        PlaceCollectionTarget target = createTarget("11210", PlaceCategory.PHARMACY, "PM9");
        ExternalPlace place = createPlace("pharmacy-1", PlaceCategory.PHARMACY);
        when(targetProvider.createTargets()).thenReturn(List.of(target));
        when(kakaoPlaceApiClient.fetchPlaces(target)).thenReturn(List.of(place));

        List<ExternalPlace> result = collectionService.collect();
        Throwable actual = catchThrowable(() -> result.add(place));

        assertThat(actual).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void apiExceptionStopsCollectionImmediately() {
        PlaceCollectionTarget pharmacyTarget = createTarget("11210", PlaceCategory.PHARMACY, "PM9");
        PlaceCollectionTarget cafeTarget = createTarget("11230", PlaceCategory.CAFE, "CE7");
        KakaoPlaceApiException exception = new KakaoPlaceApiException("api failed");
        when(targetProvider.createTargets()).thenReturn(List.of(pharmacyTarget, cafeTarget));
        when(kakaoPlaceApiClient.fetchPlaces(pharmacyTarget)).thenThrow(exception);

        Throwable actual = catchThrowable(() -> collectionService.collect());

        assertThat(actual).isSameAs(exception);
        InOrder inOrder = inOrder(targetProvider, kakaoPlaceApiClient);
        inOrder.verify(targetProvider).createTargets();
        inOrder.verify(kakaoPlaceApiClient).fetchPlaces(pharmacyTarget);
        inOrder.verifyNoMoreInteractions();
    }

    private PlaceCollectionTarget createTarget(
            String guCode,
            PlaceCategory placeCategory,
            String requestValue
    ) {
        return PlaceCollectionTarget.builder()
                .dataSourceId(13L)
                .guCode(guCode)
                .guName("test-gu")
                .searchType(KakaoPlaceSearchType.CATEGORY)
                .requestValue(requestValue)
                .placeCategory(placeCategory)
                .centerLatitude(new BigDecimal("37.478400"))
                .centerLongitude(new BigDecimal("126.951600"))
                .build();
    }

    private ExternalPlace createPlace(
            String sourcePlaceId,
            PlaceCategory placeCategory
    ) {
        return ExternalPlace.builder()
                .dataSourceId(13L)
                .sourcePlaceId(sourcePlaceId)
                .placeCategory(placeCategory)
                .name(sourcePlaceId)
                .rawCategoryCode("PM9")
                .rawCategoryName("medical > pharmacy")
                .address("Seoul")
                .latitude("37.478400")
                .longitude("126.951600")
                .build();
    }
}
