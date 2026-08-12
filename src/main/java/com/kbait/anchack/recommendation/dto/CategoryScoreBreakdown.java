package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** recommendation_scores 컬럼과 1:1 대응하는 카테고리별 점수 내역. */
@Getter
@Builder
public class CategoryScoreBreakdown {

    private String category;

    /** 해당 카테고리의 원래 저장 점수(0~100). */
    private BigDecimal rawScore;

    /** 정규화된 가중치(합 1). */
    private BigDecimal weight;

    /** weight * zScore. 음수일 수 있음. */
    private BigDecimal weightedScore;
}
