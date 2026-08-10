package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.RecommendationRow;

import java.util.List;

public interface RecommendationService {

    List<RecommendationRow> generate(ConditionBundle condition);
}
