package com.kbait.anchack.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** property_metrics 스냅샷의 admin_dong_id + rental_type + house_type 집계 행. */
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
