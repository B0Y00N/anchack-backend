package com.kbait.anchack.place.service;

import com.kbait.anchack.place.dto.external.ExternalPlace;

import java.time.LocalDate;
import java.util.List;

public interface KakaoPlaceCollectionService {

    List<ExternalPlace> collect(LocalDate dataDate);
}
