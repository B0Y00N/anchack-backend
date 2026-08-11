package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserConditionCreateResponse {

    private Long conditionId;
    private List<RecommendedDongResponse> recommendations;
}
