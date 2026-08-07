package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.AdminDongMetricScores;

import java.util.List;

public interface MetricScoreMapper {

    List<AdminDongMetricScores> findAllLatestScores();
}
