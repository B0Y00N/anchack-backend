package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

/** 하드필터/최종 추출 각 단계를 통과하고 남은 후보 수. 프론트 로딩 화면 퍼널 표시용. */
@Getter
@Builder
public class FilterFunnelStage {

    private String stage;
    private String label;
    private int count;
}
