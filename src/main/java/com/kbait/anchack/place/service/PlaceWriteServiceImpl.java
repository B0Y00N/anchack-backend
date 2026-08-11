package com.kbait.anchack.place.service;

import com.kbait.anchack.place.domain.Place;
import com.kbait.anchack.place.mapper.PlaceMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaceWriteServiceImpl implements PlaceWriteService {

    private static final int UPSERT_CHUNK_SIZE = 500;

    private final PlaceMapper placeMapper;

    @Override
    @Transactional
    public int upsertPlaces(List<Place> places) {
        if (places == null) {
            throw new IllegalArgumentException("places must not be null.");
        }
        if (places.isEmpty()) {
            return 0;
        }

        return upsertInChunks(places);
    }

    private int upsertInChunks(List<Place> places) {
        int affectedRowCount = 0;
        for (int startIndex = 0; startIndex < places.size(); startIndex += UPSERT_CHUNK_SIZE) {
            int endIndex = Math.min(startIndex + UPSERT_CHUNK_SIZE, places.size());
            affectedRowCount += placeMapper.upsertBatch(places.subList(startIndex, endIndex));
        }

        return affectedRowCount;
    }
}
