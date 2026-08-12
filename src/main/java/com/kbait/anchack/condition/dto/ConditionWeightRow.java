package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class ConditionWeightRow {

    private Long conditionId;
    private String category;
    private BigDecimal importance;
}
