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
    private Integer cctv;
    private Integer safetyBellCount;
    private BigDecimal safetyScoreMax;
    private BigDecimal crimeRate;
    private Integer hospitalCount;
    private Integer pharmacyCount;
    private Integer bankCount;
    private Integer cafeCount;
    private Integer restaurantCount;
    private Integer departmentStoreCount;
    private Integer martCount;
    private Integer gymCount;
    private Integer parkCount;
    private Integer convenienceStoreCount;
    private Integer subwayStationCount;
    private Integer busStopCount;
    private BigDecimal transitScore;
}
