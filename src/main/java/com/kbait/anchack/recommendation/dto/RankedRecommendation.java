package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** 소프트 스코어링을 마치고 순위까지 매긴 최종 결과 1건. */
@Getter
@Builder
public class RankedRecommendation {

    private Long adminDongId;
    private BigDecimal totalScore;
    private BigDecimal dataCoverageRate;
    private Integer rank;
    private Integer commuteTime;
    private Integer transferCount;
    private String route;
    private String transportType;
    private String lineNum;
    private String vehicleType;
    private Integer walkMin;
    private Integer subwayMin;
    private List<CategoryScoreBreakdown> categoryBreakdowns;
}
