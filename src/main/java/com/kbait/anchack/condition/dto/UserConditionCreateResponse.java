package com.kbait.anchack.condition.dto;

import com.kbait.anchack.recommendation.dto.FilterFunnelStage;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class UserConditionCreateResponse {

    private Long conditionId;

    /** 하드필터+최종 추출 각 단계를 통과한 후보 수(로딩 화면 퍼널 표시용, P3). 후보가 어느
     * 단계에서 0개가 되면 그 이후 실행되지 않은 단계는 배열에서 아예 빠진다. destAddress를
     * 안 보낸 검색이면 COMMUTE 단계도 빠진다(실질적 필터링이 없어서). */
    private List<FilterFunnelStage> filterFunnel;

    private List<RecommendedDongResponse> recommendations;
}
