package com.kbait.anchack.place.cctv.service;

import com.kbait.anchack.place.dto.external.ExternalPlace;

import java.io.Reader;
import java.util.List;

public interface CctvPlaceCollectionService {

    List<ExternalPlace> collect(Reader reader);
}
