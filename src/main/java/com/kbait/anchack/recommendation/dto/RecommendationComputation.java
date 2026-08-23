package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** RecommendationService.compute()의 결과. 아직 DB에 반영되지 않은 추천 행과, 그 추천이
 * 나오기까지 하드필터+최종 추출(FINAL) 각 단계를 통과한 후보 수(filterFunnel)를 함께 담는다. */
@Getter
@Builder
public class RecommendationComputation {

    private List<RecommendationRow> rows;
    private List<FilterFunnelStage> filterFunnel;
}
