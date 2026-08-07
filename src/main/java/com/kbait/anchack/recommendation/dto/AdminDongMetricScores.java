package com.kbait.anchack.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 행정동 1개의 9개 카테고리 최신 지표 점수. 값이 없으면(집계 전) null. */
@Getter
@Setter
@NoArgsConstructor
public class AdminDongMetricScores {

    private Long adminDongId;
    private BigDecimal cultureScore;
    private BigDecimal transitScore;
    private BigDecimal sportsScore;
    private BigDecimal natureScore;
    private BigDecimal convenienceScore;
    private BigDecimal healthcareScore;
    private BigDecimal foodScore;
    private BigDecimal safetyScore;
    private BigDecimal silenceScore;
}
