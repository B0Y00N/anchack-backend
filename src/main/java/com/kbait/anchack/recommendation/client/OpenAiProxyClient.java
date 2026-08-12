package com.kbait.anchack.recommendation.client;

import com.kbait.anchack.recommendation.config.OpenAiProxyProperties;
import com.kbait.anchack.recommendation.dto.openai.OpenAiChatChoice;
import com.kbait.anchack.recommendation.dto.openai.OpenAiChatMessage;
import com.kbait.anchack.recommendation.dto.openai.OpenAiChatRequest;
import com.kbait.anchack.recommendation.dto.openai.OpenAiChatResponse;
import com.kbait.anchack.recommendation.exception.OpenAiApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Objects;

/**
 * 팀 프록시 서버를 통한 OpenAI Chat Completions 호출. 프록시가 OpenAI와 동일한 요청/응답
 * 포맷(POST {baseUrl}/v1/chat/completions, Authorization: Bearer {token})을 쓰므로
 * 엔드포인트/인증만 프록시 값으로 바꿔 호출한다.
 */
public final class OpenAiProxyClient {

    private static final String CHAT_COMPLETIONS_PATH = "/v1/chat/completions";
    private static final String AUTHORIZATION_PREFIX = "Bearer ";

    private final RestTemplate restTemplate;
    private final OpenAiProxyProperties properties;

    public OpenAiProxyClient(RestTemplate restTemplate, OpenAiProxyProperties properties) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate는 null일 수 없습니다.");
        this.properties = Objects.requireNonNull(properties, "properties는 null일 수 없습니다.");
    }

    public String chat(String prompt) {
        validateApiToken();

        OpenAiChatResponse response = fetch(prompt);
        List<OpenAiChatChoice> choices = response.getChoices();

        if (choices.isEmpty()) {
            throw new OpenAiApiException("OpenAI 프록시 응답에 choices가 없습니다.");
        }

        return choices.get(0).getMessage().getContent();
    }

    private OpenAiChatResponse fetch(String prompt) {
        OpenAiChatRequest requestBody = OpenAiChatRequest.builder()
                .model(properties.getModel())
                .messages(List.of(OpenAiChatMessage.builder().role("user").content(prompt).build()))
                .maxTokens(properties.getMaxTokens())
                .build();

        HttpEntity<OpenAiChatRequest> request = new HttpEntity<>(requestBody, createHeaders());

        try {
            return restTemplate.postForObject(
                    properties.getBaseUrl() + CHAT_COMPLETIONS_PATH, request, OpenAiChatResponse.class);
        } catch (RestClientResponseException exception) {
            throw new OpenAiApiException(
                    "OpenAI 프록시 호출 실패: httpStatus=" + exception.getRawStatusCode());
        } catch (RestClientException exception) {
            throw new OpenAiApiException("OpenAI 프록시 호출 실패", exception);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + properties.getApiToken());

        return headers;
    }

    private void validateApiToken() {
        if (!StringUtils.hasText(properties.getApiToken())) {
            throw new OpenAiApiException("OpenAI 프록시 토큰이 설정되지 않았습니다.");
        }
    }
}
