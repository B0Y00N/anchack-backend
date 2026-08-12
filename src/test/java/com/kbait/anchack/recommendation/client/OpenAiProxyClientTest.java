package com.kbait.anchack.recommendation.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.recommendation.config.OpenAiProxyProperties;
import com.kbait.anchack.recommendation.dto.openai.OpenAiChatResponse;
import com.kbait.anchack.recommendation.exception.OpenAiApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiProxyClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Mock
    private RestTemplate restTemplate;

    private OpenAiProxyClient client;

    @BeforeEach
    void setUp() {
        OpenAiProxyProperties properties = new OpenAiProxyProperties(
                "https://proxy.example.com", "team-token", "gpt-4o-mini", 200, 5000, 15000);
        client = new OpenAiProxyClient(restTemplate, properties);
    }

    @Test
    void 정상_응답이면_content를_그대로_반환한다() throws Exception {
        stubResponse("""
                {"choices": [{"message": {"content": "안전 점수가 높아요"}}]}
                """);

        String content = client.chat("프롬프트");

        assertThat(content).isEqualTo("안전 점수가 높아요");
    }

    @Test
    void 응답_자체가_null이면_예외를_던진다() {
        when(restTemplate.postForObject(any(String.class), any(), eq(OpenAiChatResponse.class)))
                .thenReturn(null);

        assertThatThrownBy(() -> client.chat("프롬프트")).isInstanceOf(OpenAiApiException.class);
    }

    @Test
    void choices가_비어있으면_예외를_던진다() throws Exception {
        stubResponse("""
                {"choices": []}
                """);

        assertThatThrownBy(() -> client.chat("프롬프트")).isInstanceOf(OpenAiApiException.class);
    }

    @Test
    void choices의_첫_원소가_null이면_예외를_던진다() throws Exception {
        stubResponse("""
                {"choices": [null]}
                """);

        assertThatThrownBy(() -> client.chat("프롬프트")).isInstanceOf(OpenAiApiException.class);
    }

    @Test
    void message가_null이면_예외를_던진다() throws Exception {
        stubResponse("""
                {"choices": [{"message": null}]}
                """);

        assertThatThrownBy(() -> client.chat("프롬프트")).isInstanceOf(OpenAiApiException.class);
    }

    @Test
    void content가_null이면_예외를_던진다() throws Exception {
        stubResponse("""
                {"choices": [{"message": {"content": null}}]}
                """);

        assertThatThrownBy(() -> client.chat("프롬프트")).isInstanceOf(OpenAiApiException.class);
    }

    private void stubResponse(String json) throws Exception {
        OpenAiChatResponse response = OBJECT_MAPPER.readValue(json, OpenAiChatResponse.class);

        when(restTemplate.postForObject(any(String.class), any(), eq(OpenAiChatResponse.class)))
                .thenReturn(response);
    }
}
