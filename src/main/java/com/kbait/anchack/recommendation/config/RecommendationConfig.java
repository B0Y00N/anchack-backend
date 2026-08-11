package com.kbait.anchack.recommendation.config;

import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.client.impl.OpenAiRecommendationReasonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RecommendationConfig {

    @Bean
    public RecommendationReasonClient recommendationReasonClient() {
        return new OpenAiRecommendationReasonClient();
    }
}
