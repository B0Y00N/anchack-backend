package com.kbait.anchack.place.cctv.service;

import com.kbait.anchack.place.cctv.client.CctvCsvDownloadClient;
import com.kbait.anchack.place.cctv.exception.CctvCsvDownloadException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.Reader;
import java.io.StringReader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CctvPlaceIngestionRunnerTest {

    @Mock
    private CctvCsvDownloadClient csvDownloadClient;

    @Mock
    private CctvPlaceIngestionService ingestionService;

    @Test
    void runDownloadsCsvAndPassesReaderToIngestionService() {
        TrackingReader reader = new TrackingReader("cctv csv");
        CctvPlaceIngestionResult expected = new CctvPlaceIngestionResult(2, 1, 1, 1);
        when(csvDownloadClient.download()).thenReturn(reader);
        when(ingestionService.ingest(reader)).thenReturn(expected);
        CctvPlaceIngestionRunner runner = createRunner();

        CctvPlaceIngestionResult actual = runner.run();

        assertThat(actual).isSameAs(expected);
        verify(ingestionService).ingest(reader);
        assertThat(reader.isClosed()).isTrue();
    }

    @Test
    void runStopsWhenCsvDownloadFails() {
        CctvCsvDownloadException exception = new CctvCsvDownloadException("download failed");
        when(csvDownloadClient.download()).thenThrow(exception);
        CctvPlaceIngestionRunner runner = createRunner();

        Throwable actual = catchThrowable(runner::run);

        assertThat(actual).isSameAs(exception);
        verifyNoInteractions(ingestionService);
    }

    @Test
    void runStopsWhenIngestionFails() {
        Reader reader = new StringReader("cctv csv");
        IllegalStateException exception = new IllegalStateException("upsert failed");
        when(csvDownloadClient.download()).thenReturn(reader);
        when(ingestionService.ingest(reader)).thenThrow(exception);
        CctvPlaceIngestionRunner runner = createRunner();

        Throwable actual = catchThrowable(runner::run);

        assertThat(actual).isSameAs(exception);
    }

    private CctvPlaceIngestionRunner createRunner() {
        return new CctvPlaceIngestionRunnerImpl(csvDownloadClient, ingestionService);
    }

    private static final class TrackingReader extends StringReader {

        private boolean closed;

        private TrackingReader(String value) {
            super(value);
        }

        @Override
        public void close() {
            closed = true;
            super.close();
        }

        private boolean isClosed() {
            return closed;
        }
    }
}
