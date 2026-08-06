package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.LifeConvenienceMetricRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface LifeConvenienceMetricMapper {

    List<LifeConvenienceMetricRow> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<LifeConvenienceMetricRow> rows);
}
