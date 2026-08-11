package com.kbait.anchack.place.service;

import com.kbait.anchack.place.domain.Place;

import java.util.List;

public interface PlaceWriteService {

    int upsertPlaces(List<Place> places);
}
