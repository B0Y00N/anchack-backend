package com.kbait.anchack.place.mapper;

import com.kbait.anchack.place.domain.Place;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PlaceMapper {

    int upsertBatch(@Param("places") List<Place> places);

    List<Place> findByCategory(@Param("category") String category);
}
