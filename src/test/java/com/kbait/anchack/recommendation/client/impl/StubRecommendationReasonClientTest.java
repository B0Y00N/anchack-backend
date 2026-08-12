package com.kbait.anchack.recommendation.client.impl;

import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RecommendationReasonContext;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StubRecommendationReasonClientTest {

    @Test
    void 항상_플레이스홀더_문구를_반환한다() {
        StubRecommendationReasonClient client = new StubRecommendationReasonClient();

        GeneratedReason result = client.generate(RecommendationReasonContext.builder().adminDongId(1L).build());

        assertThat(result.getRecommendationReason()).isEqualTo("추후 openai api 호출");
        assertThat(result.getCaution()).isEqualTo("추후 openai api 호출");
    }
}
