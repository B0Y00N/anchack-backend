package com.kbait.anchack.place.mapper;

import com.kbait.anchack.place.domain.Place;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PlaceMapper {

    int upsertBatch(@Param("places") List<Place> places);

    List<Place> findByCategory(@Param("category") String category);

    /** categories가 null/비어있으면 카테고리 제한 없이 해당 행정동의 전체 장소를 반환한다. */
    List<Place> findByAdminDongIdAndCategories(
            @Param("adminDongId") Long adminDongId,
            @Param("categories") List<String> categories
    );
}
