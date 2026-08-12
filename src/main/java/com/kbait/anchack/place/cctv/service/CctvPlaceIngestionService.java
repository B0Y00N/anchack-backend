package com.kbait.anchack.place.cctv.service;

import java.io.Reader;

public interface CctvPlaceIngestionService {

    CctvPlaceIngestionResult ingest(Reader reader);
}
