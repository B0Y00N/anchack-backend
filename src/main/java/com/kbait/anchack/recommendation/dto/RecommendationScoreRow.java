package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** recommendation_scores INSERT 페이로드. */
@Getter
@Builder
public class RecommendationScoreRow {

    private Long recommendationId;
    private String category;
    private BigDecimal rawScore;
    private BigDecimal weight;
    private BigDecimal weightedScore;
}
