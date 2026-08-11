package com.kbait.anchack.place.service;

import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.mapper.PlaceMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.anyList;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceWriteServiceTest {

    @Mock
    private PlaceMapper placeMapper;

    private PlaceWriteService placeWriteService;

    @BeforeEach
    void setUp() {
        placeWriteService = new PlaceWriteServiceImpl(placeMapper);
    }

    @Test
    void upsertsPlacesInSingleBatch() {
        List<Place> places = List.of(createPlace(1), createPlace(2));
        when(placeMapper.upsertBatch(places)).thenReturn(2);

        int actual = placeWriteService.upsertPlaces(places);

        assertThat(actual).isEqualTo(2);
    }

    @Test
    void emptyPlacesDoNotCallMapper() {
        int actual = placeWriteService.upsertPlaces(List.of());

        assertThat(actual).isZero();
        verifyNoInteractions(placeMapper);
    }

    @Test
    void nullPlacesFailBeforeMapperCall() {
        Throwable actual = catchThrowable(() -> placeWriteService.upsertPlaces(null));

        assertThat(actual).isExactlyInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(placeMapper);
    }

    @Test
    void upsertsPlacesInChunks() {
        List<Place> places = IntStream.rangeClosed(1, 1_001)
                .mapToObj(this::createPlace)
                .toList();
        when(placeMapper.upsertBatch(anyList()))
                .thenAnswer(invocation -> ((List<?>) invocation.getArgument(0)).size());
        ArgumentCaptor<List<Place>> captor = placeListCaptor();

        int actual = placeWriteService.upsertPlaces(places);

        assertThat(actual).isEqualTo(1_001);
        verify(placeMapper, times(3)).upsertBatch(captor.capture());
        assertThat(captor.getAllValues())
                .extracting(List::size)
                .containsExactly(500, 500, 1);
    }

    private Place createPlace(int sequence) {
        return Place.builder()
                .externalId("external-" + sequence)
                .adminDongId(1L)
                .category(PlaceCategory.PHARMACY)
                .name("place-" + sequence)
                .latitude(new BigDecimal("37.478154"))
                .longitude(new BigDecimal("126.951484"))
                .dataSourceId(13L)
                .build();
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<List<Place>> placeListCaptor() {
        return ArgumentCaptor.forClass(List.class);
    }
}
