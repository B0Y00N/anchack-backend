package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PreferredHouseTypeRow {

    private Long conditionId;
    private String houseType;
}
