package com.kbait.anchack.admindong.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** AdminDongDetailMapper.findMetricsByIds 조회 결과 1행. */
@Getter
@Setter
public class AdminDongMetricsRow {

    private Long adminDongId;
    private BigDecimal safetyScore;
    private BigDecimal cctvPer1000;
    private BigDecimal cctvPer100m;
    private BigDecimal crimeRate;
    private Integer hospitalCount;
    private Integer departmentStoreCount;
    private Integer martCount;
    private Integer gymCount;
    private Integer parkCount;
    private Integer convenienceStoreCount;
}
