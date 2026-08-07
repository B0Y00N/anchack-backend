package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.SportsMetricRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SportsMetricMapper {

    List<SportsMetricRow> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<SportsMetricRow> rows);
}
