package com.kbait.anchack.admindong.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** property_metrics에서 상세 화면에 노출할 주거유형별 집계 행. */
@Getter
@Setter
public class HouseTypeMetricRow {

    private Long adminDongId;
    private String rentalType;
    private String houseType;
    private BigDecimal avgArea;
    private Long avgDeposit;
    private Long avgRent;
    private Integer transactionCount;
}
