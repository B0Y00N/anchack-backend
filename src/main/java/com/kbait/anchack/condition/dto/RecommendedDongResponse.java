package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RecommendedDongResponse {

    private Long adminDongId;
    private BigDecimal totalScore;
    private BigDecimal dataCoverageRate;
    private Integer rank;
    private Integer commuteTime;
    private Integer transferCount;
    private String recommendationReason;
    private String caution;
}
