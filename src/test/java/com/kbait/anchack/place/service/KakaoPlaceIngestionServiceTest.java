package com.kbait.anchack.place.service;

import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.exception.KakaoPlaceApiException;
import com.kbait.anchack.place.normalizer.PlaceNormalizer;
import com.kbait.anchack.place.resolver.PlaceAdminDongResolver;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoPlaceIngestionServiceTest {

    @Mock
    private KakaoPlaceCollectionService kakaoPlaceCollectionService;

    @Mock
    private PlaceAdminDongResolver placeAdminDongResolver;

    @Mock
    private PlaceWriteService placeWriteService;

    private KakaoPlaceIngestionService kakaoPlaceIngestionService;

    @BeforeEach
    void setUp() {
        kakaoPlaceIngestionService = new KakaoPlaceIngestionServiceImpl(
                kakaoPlaceCollectionService,
                new PlaceNormalizer(),
                placeAdminDongResolver,
                placeWriteService
        );
    }

    @Test
    void ingestsCollectedPlaces() {
        ExternalPlace firstPlace = validPlaceBuilder()
                .sourcePlaceId("kakao-1")
                .name("first pharmacy")
                .build();
        ExternalPlace secondPlace = validPlaceBuilder()
                .sourcePlaceId("kakao-2")
                .name("second pharmacy")
                .build();
        when(kakaoPlaceCollectionService.collect()).thenReturn(List.of(firstPlace, secondPlace));
        when(placeAdminDongResolver.resolveAdminDongId(firstPlace)).thenReturn(Optional.of(101L));
        when(placeAdminDongResolver.resolveAdminDongId(secondPlace)).thenReturn(Optional.of(102L));
        when(placeWriteService.upsertPlaces(anyList())).thenReturn(2);
        ArgumentCaptor<List<Place>> captor = placeListCaptor();

        KakaoPlaceIngestionResult actual = kakaoPlaceIngestionService.ingest();

        assertThat(actual.getCollectedCount()).isEqualTo(2);
        assertThat(actual.getNormalizedCount()).isEqualTo(2);
        assertThat(actual.getSkippedCount()).isZero();
        assertThat(actual.getAffectedRowCount()).isEqualTo(2);
        verify(placeWriteService).upsertPlaces(captor.capture());
        assertThat(captor.getValue())
                .extracting(Place::getAdminDongId)
                .containsExactly(101L, 102L);
    }

    @Test
    void skipsInvalidAndUnresolvedPlaces() {
        ExternalPlace validPlace = validPlaceBuilder()
                .sourcePlaceId("kakao-1")
                .name("valid pharmacy")
                .build();
        ExternalPlace invalidPlace = validPlaceBuilder()
                .sourcePlaceId("kakao-2")
                .name("")
                .build();
        ExternalPlace unresolvedPlace = validPlaceBuilder()
                .sourcePlaceId("kakao-3")
                .name("unresolved pharmacy")
                .build();
        when(kakaoPlaceCollectionService.collect()).thenReturn(List.of(validPlace, invalidPlace, unresolvedPlace));
        when(placeAdminDongResolver.resolveAdminDongId(validPlace)).thenReturn(Optional.of(101L));
        when(placeAdminDongResolver.resolveAdminDongId(unresolvedPlace)).thenReturn(Optional.empty());
        when(placeWriteService.upsertPlaces(anyList())).thenReturn(1);
        ArgumentCaptor<List<Place>> captor = placeListCaptor();

        KakaoPlaceIngestionResult actual = kakaoPlaceIngestionService.ingest();

        assertThat(actual.getCollectedCount()).isEqualTo(3);
        assertThat(actual.getNormalizedCount()).isEqualTo(1);
        assertThat(actual.getSkippedCount()).isEqualTo(2);
        assertThat(actual.getAffectedRowCount()).isEqualTo(1);
        verify(placeWriteService).upsertPlaces(captor.capture());
        assertThat(captor.getValue())
                .extracting(Place::getExternalId)
                .containsExactly("kakao-1");
    }

    @Test
    void writesEmptyListWhenCollectedPlacesAreEmpty() {
        when(kakaoPlaceCollectionService.collect()).thenReturn(List.of());
        when(placeWriteService.upsertPlaces(List.of())).thenReturn(0);

        KakaoPlaceIngestionResult actual = kakaoPlaceIngestionService.ingest();

        assertThat(actual.getCollectedCount()).isZero();
        assertThat(actual.getNormalizedCount()).isZero();
        assertThat(actual.getSkippedCount()).isZero();
        assertThat(actual.getAffectedRowCount()).isZero();
        verifyNoInteractions(placeAdminDongResolver);
    }

    @Test
    void apiExceptionStopsIngestion() {
        KakaoPlaceApiException exception = new KakaoPlaceApiException("api failed");
        when(kakaoPlaceCollectionService.collect()).thenThrow(exception);

        Throwable actual = catchThrowable(() -> kakaoPlaceIngestionService.ingest());

        assertThat(actual).isSameAs(exception);
        verifyNoInteractions(placeAdminDongResolver, placeWriteService);
    }

    private ExternalPlace.ExternalPlaceBuilder validPlaceBuilder() {
        return ExternalPlace.builder()
                .dataSourceId(13L)
                .sourcePlaceId("kakao-123")
                .placeCategory(PlaceCategory.PHARMACY)
                .name("pharmacy")
                .rawCategoryCode("PM9")
                .rawCategoryName("medical > pharmacy")
                .address("Seoul Gwanak-gu Bongcheon-dong")
                .roadAddress("Seoul Gwanak-gu Gwanak-ro 1")
                .latitude("37.478154")
                .longitude("126.951484");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<Place>> placeListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
