package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.TransitMetricRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface TransitMetricMapper {

    List<TransitMetricRow> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<TransitMetricRow> rows);
}
