package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.PropertyMetricCandidateRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PropertyMetricMapper {

    List<PropertyMetricCandidateRow> findLatestByDongsRentalTypeAndHouseTypes(
            @Param("adminDongIds") List<Long> adminDongIds,
            @Param("rentalType") String rentalType,
            @Param("houseTypes") List<String> houseTypes
    );
}
