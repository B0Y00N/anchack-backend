package com.kbait.anchack.place.scheduler;

import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionResult;
import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionRunner;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CctvPlaceIngestionSchedulerTest {

    @Mock
    private CctvPlaceIngestionRunner cctvPlaceIngestionRunner;

    @Test
    void ingestPlacesCallsCctvPlaceIngestionRunner() {
        CctvPlaceIngestionResult result = new CctvPlaceIngestionResult(10, 8, 2, 8);
        when(cctvPlaceIngestionRunner.run()).thenReturn(result);
        CctvPlaceIngestionScheduler scheduler = createScheduler();

        scheduler.ingestPlaces();

        verify(cctvPlaceIngestionRunner).run();
    }

    @Test
    void ingestPlacesHandlesIngestionFailure() {
        when(cctvPlaceIngestionRunner.run()).thenThrow(new IllegalStateException("ingestion failed"));
        CctvPlaceIngestionScheduler scheduler = createScheduler();

        assertThatCode(scheduler::ingestPlaces).doesNotThrowAnyException();

        verify(cctvPlaceIngestionRunner).run();
    }

    private CctvPlaceIngestionScheduler createScheduler() {
        return new CctvPlaceIngestionScheduler(cctvPlaceIngestionRunner);
    }
}
