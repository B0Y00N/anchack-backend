package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ConditionGuRow {

    private Long conditionId;
    private String guCode;
}
