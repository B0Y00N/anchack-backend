package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.CultureMetricRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface CultureMetricMapper {

    List<CultureMetricRow> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<CultureMetricRow> rows);
}
