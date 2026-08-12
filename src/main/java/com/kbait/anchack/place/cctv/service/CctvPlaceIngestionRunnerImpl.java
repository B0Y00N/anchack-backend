package com.kbait.anchack.place.cctv.service;

import com.kbait.anchack.place.cctv.client.CctvCsvDownloadClient;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.Reader;

@RequiredArgsConstructor
public class CctvPlaceIngestionRunnerImpl implements CctvPlaceIngestionRunner {

    private final CctvCsvDownloadClient csvDownloadClient;
    private final CctvPlaceIngestionService ingestionService;

    @Override
    public CctvPlaceIngestionResult run() {
        try (Reader reader = csvDownloadClient.download()) {
            return ingestionService.ingest(reader);
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to close downloaded CCTV CSV reader.", exception);
        }
    }
}
