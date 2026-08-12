package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** OpenAI 프롬프트 구성에 필요한 최소 정보. */
@Getter
@Builder
public class RecommendationReasonContext {

    private Long adminDongId;
    private Long conditionId;
    private BigDecimal totalScore;
    private Integer commuteTime;
    private Integer transferCount;
}
