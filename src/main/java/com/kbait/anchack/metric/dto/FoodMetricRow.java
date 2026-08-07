package com.kbait.anchack.metric.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** places를 집계해서 만든 food_metrics 신규 행 (MyBatis 조회 결과 매핑 + INSERT 입력 겸용) */
@Getter
@Setter
@NoArgsConstructor
public class FoodMetricRow {

    private Long adminDongId;
    private BigDecimal dongPopulation;
    private BigDecimal dongArea;
    private Long restaurantCount;
    private Long cafeCount;
    private BigDecimal score;
}
