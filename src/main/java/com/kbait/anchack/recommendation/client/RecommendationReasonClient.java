package com.kbait.anchack.recommendation.client;

import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RecommendationReasonContext;

public interface RecommendationReasonClient {

    GeneratedReason generate(RecommendationReasonContext context);
}
