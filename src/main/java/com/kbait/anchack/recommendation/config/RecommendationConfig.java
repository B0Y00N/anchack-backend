package com.kbait.anchack.recommendation.config;

import com.kbait.anchack.recommendation.client.OpenAiProxyClient;
import com.kbait.anchack.recommendation.client.RecommendationReasonClient;
import com.kbait.anchack.recommendation.client.impl.OpenAiRecommendationReasonClient;
import com.kbait.anchack.recommendation.client.impl.StubRecommendationReasonClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RecommendationConfig {

    private static final String MODE_OPENAI = "openai";

    @Bean
    public OpenAiProxyProperties openAiProxyProperties(
            @Value("${openai.proxy.base-url:${OPENAI_PROXY_BASE_URL:}}") String baseUrl,
            @Value("${openai.proxy.token:${OPENAI_PROXY_TOKEN:}}") String apiToken,
            @Value("${openai.proxy.model:${OPENAI_PROXY_MODEL:gpt-4o-mini}}") String model,
            @Value("${openai.proxy.max-tokens:${OPENAI_PROXY_MAX_TOKENS:200}}") int maxTokens,
            @Value("${openai.proxy.connect-timeout-ms:${OPENAI_PROXY_CONNECT_TIMEOUT_MS:5000}}") int connectTimeoutMs,
            @Value("${openai.proxy.read-timeout-ms:${OPENAI_PROXY_READ_TIMEOUT_MS:15000}}") int readTimeoutMs
    ) {
        return new OpenAiProxyProperties(baseUrl, apiToken, model, maxTokens, connectTimeoutMs, readTimeoutMs);
    }

    @Bean(name = "openAiProxyRestTemplate")
    public RestTemplate openAiProxyRestTemplate(OpenAiProxyProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        return new RestTemplate(requestFactory);
    }

    @Bean
    public OpenAiProxyClient openAiProxyClient(
            @Qualifier("openAiProxyRestTemplate") RestTemplate restTemplate,
            OpenAiProxyProperties properties
    ) {
        return new OpenAiProxyClient(restTemplate, properties);
    }

    /**
     * recommendation.reason.mode(기본 stub)로 실제 OpenAI 호출과 플레이스홀더 스텁을
     * 코드 변경 없이 전환한다. 토큰 예산이 엄격해서 평소 개발/테스트는 stub으로 하고,
     * 필요할 때만 .env의 RECOMMENDATION_REASON_MODE=openai로 바꿔 재기동한다.
     */
    @Bean
    public RecommendationReasonClient recommendationReasonClient(
            @Value("${recommendation.reason.mode:${RECOMMENDATION_REASON_MODE:stub}}") String mode,
            OpenAiProxyClient openAiProxyClient
    ) {
        if (MODE_OPENAI.equalsIgnoreCase(mode)) {
            return new OpenAiRecommendationReasonClient(openAiProxyClient);
        }

        return new StubRecommendationReasonClient();
    }
}
