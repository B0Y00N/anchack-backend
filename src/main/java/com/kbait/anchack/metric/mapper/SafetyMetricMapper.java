package com.kbait.anchack.metric.mapper;

import com.kbait.anchack.metric.dto.SafetyScoreInput;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface SafetyMetricMapper {

    List<SafetyScoreInput> findAllCountsFromPlaces();

    void insertRows(@Param("rows") List<SafetyScoreInput> rows);
}
