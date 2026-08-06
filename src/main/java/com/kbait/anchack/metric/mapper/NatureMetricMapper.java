package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.NatureMetricRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface NatureMetricMapper {

    List<NatureMetricRow> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<NatureMetricRow> rows);
}
