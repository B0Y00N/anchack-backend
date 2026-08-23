package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

/** HardFilterService.filter()의 결과. 최종 후보와 함께, 각 필터 단계를 통과하고 남은
 * 후보 수(filterFunnel)도 같이 담는다 - 응답 조립 단계(RecommendationServiceImpl)에서
 * FINAL 단계까지 이어붙인 뒤 그대로 API 응답에 실린다. */
@Getter
@Builder
public class HardFilterResult {

    private List<RecommendationCandidate> candidates;
    private List<FilterFunnelStage> filterFunnel;
}
