package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.AdminDongCategoryRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface EssentialInfraMapper {

    List<AdminDongCategoryRow> findCategoriesByDongsAndCategories(
            @Param("adminDongIds") List<Long> adminDongIds,
            @Param("categories") List<String> categories
    );
}
