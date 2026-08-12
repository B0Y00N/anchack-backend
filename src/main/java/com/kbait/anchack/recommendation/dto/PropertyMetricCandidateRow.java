package com.kbait.anchack.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** admin_dong_id + house_type별 최신 property_metrics 한 건. 주거유형·예산 하드필터 판정용. */
@Getter
@Setter
@NoArgsConstructor
public class PropertyMetricCandidateRow {

    private Long adminDongId;
    private String houseType;
    private Long avgDeposit;
    private Long avgRent;
    private BigDecimal avgArea;
}
