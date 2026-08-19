package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RecommendedDongResponse {

    private Long adminDongId;
    private String guName;
    private String dongName;
    private BigDecimal lat;
    private BigDecimal lng;
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
    private String recommendationReason;
    private String caution;
}
