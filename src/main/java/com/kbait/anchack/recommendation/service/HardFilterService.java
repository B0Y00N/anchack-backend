package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.RecommendationCandidate;

import java.util.List;

public interface HardFilterService {

    List<RecommendationCandidate> filter(ConditionBundle condition);
}
