package com.kbait.anchack.admindong.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

/** 행정동 내 임대 유형별 주거유형 거래 지표. */
@Getter
@Builder
public class HouseTypeMetricResponse {

    private String houseType;
    private BigDecimal avgArea;
    private Long avgDeposit;
    private Long avgRent;
    private Integer transactionCount;
}
