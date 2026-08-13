package com.kbait.anchack.place.scheduler;

import com.kbait.anchack.place.service.KakaoPlaceIngestionResult;
import com.kbait.anchack.place.service.KakaoPlaceIngestionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoPlaceIngestionSchedulerTest {

    @Mock
    private KakaoPlaceIngestionService kakaoPlaceIngestionService;

    @Test
    void ingestPlacesCallsKakaoPlaceIngestionService() {
        KakaoPlaceIngestionResult result = new KakaoPlaceIngestionResult(10, 8, 2, 8);
        when(kakaoPlaceIngestionService.ingest()).thenReturn(result);
        KakaoPlaceIngestionScheduler scheduler = createScheduler();

        scheduler.ingestPlaces();

        verify(kakaoPlaceIngestionService).ingest();
    }

    @Test
    void ingestPlacesHandlesIngestionFailure() {
        when(kakaoPlaceIngestionService.ingest()).thenThrow(new IllegalStateException("ingestion failed"));
        KakaoPlaceIngestionScheduler scheduler = createScheduler();

        assertThatCode(scheduler::ingestPlaces).doesNotThrowAnyException();

        verify(kakaoPlaceIngestionService).ingest();
    }

    private KakaoPlaceIngestionScheduler createScheduler() {
        return new KakaoPlaceIngestionScheduler(kakaoPlaceIngestionService);
    }
}
