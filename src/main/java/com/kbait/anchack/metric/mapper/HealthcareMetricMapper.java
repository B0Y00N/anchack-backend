package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.HealthcareMetricRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface HealthcareMetricMapper {

    List<HealthcareMetricRow> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<HealthcareMetricRow> rows);
}
