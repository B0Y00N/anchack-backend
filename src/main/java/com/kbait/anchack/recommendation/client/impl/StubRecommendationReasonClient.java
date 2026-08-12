package com.kbait.anchack.recommendation.client.impl;

import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RecommendationReasonContext;

/**
 * recommendation_reason/caution을 고정 문구로 채우는 스텁. OpenAI 프록시 토큰 비용 없이
 * 나머지 추천 파이프라인을 테스트할 때 쓴다 - recommendation.reason.mode 프로퍼티가
 * "stub"(기본값)일 때 RecommendationConfig가 이 구현을 등록한다.
 */
public class StubRecommendationReasonClient implements RecommendationReasonClient {

    private static final String PLACEHOLDER = "추후 openai api 호출";

    @Override
    public GeneratedReason generate(RecommendationReasonContext context) {
        return GeneratedReason.builder()
                .recommendationReason(PLACEHOLDER)
                .caution(PLACEHOLDER)
                .build();
    }
}
