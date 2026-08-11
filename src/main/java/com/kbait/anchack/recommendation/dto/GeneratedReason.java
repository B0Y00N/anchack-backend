package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class GeneratedReason {

    private String recommendationReason;
    private String caution;
}
