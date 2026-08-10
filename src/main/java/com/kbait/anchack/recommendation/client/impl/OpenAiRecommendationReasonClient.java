package com.kbait.anchack.recommendation.client.impl;

import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RecommendationReasonContext;

/**
 * recommendation_reason/caution 생성 뼈대. 프롬프트 설계·OpenAI 호출은 아직 붙이지 않아
 * 고정 문구를 반환한다 - ingestion.client.MolitRentApiClient처럼 client는 컴포넌트
 * 스캔 대상이 아니라서 recommendation.config.RecommendationConfig의 @Bean으로 등록한다.
 */
public class OpenAiRecommendationReasonClient implements RecommendationReasonClient {

    private static final String PLACEHOLDER = "추후 openai api 호출";

    @Override
    public GeneratedReason generate(RecommendationReasonContext context) {
        return GeneratedReason.builder()
                .recommendationReason(PLACEHOLDER)
                .caution(PLACEHOLDER)
                .build();
    }
}
