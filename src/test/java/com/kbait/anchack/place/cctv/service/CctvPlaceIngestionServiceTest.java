package com.kbait.anchack.place.cctv.service;

import com.kbait.anchack.place.cctv.exception.PublicCctvCsvParseException;
import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.normalizer.PlaceNormalizer;
import com.kbait.anchack.place.resolver.PlaceAdminDongResolver;
import com.kbait.anchack.place.service.PlaceWriteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Reader;
import java.io.StringReader;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CctvPlaceIngestionServiceTest {

    @Mock
    private CctvPlaceCollectionService collectionService;

    @Mock
    private PlaceAdminDongResolver placeAdminDongResolver;

    @Mock
    private PlaceWriteService placeWriteService;

    private CctvPlaceIngestionService ingestionService;

    @BeforeEach
    void setUp() {
        ingestionService = new CctvPlaceIngestionServiceImpl(
                collectionService,
                new PlaceNormalizer(),
                placeAdminDongResolver,
                placeWriteService
        );
    }

    @Test
    void ingestNormalizesResolvesAndUpsertsCollectedPlaces() {
        ExternalPlace firstPlace = validPlaceBuilder()
                .sourcePlaceId("202630000000800463")
                .name("202630000000800463")
                .build();
        ExternalPlace secondPlace = validPlaceBuilder()
                .sourcePlaceId("202630000000800464")
                .name("202630000000800464")
                .build();
        Reader reader = new StringReader("");
        when(collectionService.collect(reader)).thenReturn(List.of(firstPlace, secondPlace));
        when(placeAdminDongResolver.resolveAdminDongId(firstPlace)).thenReturn(Optional.of(101L));
        when(placeAdminDongResolver.resolveAdminDongId(secondPlace)).thenReturn(Optional.of(102L));
        when(placeWriteService.upsertPlaces(anyList())).thenReturn(2);
        ArgumentCaptor<List<Place>> captor = placeListCaptor();

        CctvPlaceIngestionResult actual = ingestionService.ingest(reader);

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
    void ingestSkipsInvalidAndUnresolvedPlaces() {
        ExternalPlace validPlace = validPlaceBuilder()
                .sourcePlaceId("202630000000800463")
                .name("202630000000800463")
                .build();
        ExternalPlace invalidPlace = validPlaceBuilder()
                .sourcePlaceId("202630000000800464")
                .name("")
                .build();
        ExternalPlace unresolvedPlace = validPlaceBuilder()
                .sourcePlaceId("202630000000800465")
                .name("202630000000800465")
                .build();
        Reader reader = new StringReader("");
        when(collectionService.collect(reader)).thenReturn(List.of(validPlace, invalidPlace, unresolvedPlace));
        when(placeAdminDongResolver.resolveAdminDongId(validPlace)).thenReturn(Optional.of(101L));
        when(placeAdminDongResolver.resolveAdminDongId(unresolvedPlace)).thenReturn(Optional.empty());
        when(placeWriteService.upsertPlaces(anyList())).thenReturn(1);
        ArgumentCaptor<List<Place>> captor = placeListCaptor();

        CctvPlaceIngestionResult actual = ingestionService.ingest(reader);

        assertThat(actual.getCollectedCount()).isEqualTo(3);
        assertThat(actual.getNormalizedCount()).isEqualTo(1);
        assertThat(actual.getSkippedCount()).isEqualTo(2);
        assertThat(actual.getAffectedRowCount()).isEqualTo(1);
        verify(placeWriteService).upsertPlaces(captor.capture());
        assertThat(captor.getValue())
                .extracting(Place::getExternalId)
                .containsExactly("202630000000800463");
    }

    @Test
    void ingestStopsWhenCsvCollectionFails() {
        Reader reader = new StringReader("");
        PublicCctvCsvParseException exception = new PublicCctvCsvParseException("CSV parsing failed");
        when(collectionService.collect(reader)).thenThrow(exception);

        Throwable actual = catchThrowable(() -> ingestionService.ingest(reader));

        assertThat(actual).isSameAs(exception);
        verifyNoInteractions(placeAdminDongResolver, placeWriteService);
    }

    private ExternalPlace.ExternalPlaceBuilder validPlaceBuilder() {
        return ExternalPlace.builder()
                .dataSourceId(6L)
                .sourcePlaceId("202630000000800463")
                .placeCategory(PlaceCategory.CCTV)
                .name("202630000000800463")
                .address("서울특별시 종로구 삼청동 산2-1")
                .roadAddress("서울특별시 종로구 북촌로 134-1")
                .latitude("37.58785")
                .longitude("126.9843");
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<Place>> placeListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
