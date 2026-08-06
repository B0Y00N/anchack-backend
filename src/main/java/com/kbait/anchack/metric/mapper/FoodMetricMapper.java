package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.FoodMetricRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface FoodMetricMapper {

    List<FoodMetricRow> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<FoodMetricRow> rows);
}
