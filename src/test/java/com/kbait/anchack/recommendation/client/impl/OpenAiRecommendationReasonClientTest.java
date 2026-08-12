package com.kbait.anchack.recommendation.client.impl;

import com.kbait.anchack.recommendation.client.OpenAiProxyClient;
import com.kbait.anchack.recommendation.dto.CategoryScoreBreakdown;
import com.kbait.anchack.recommendation.dto.GeneratedReason;
import com.kbait.anchack.recommendation.dto.RecommendationReasonContext;
import com.kbait.anchack.recommendation.exception.OpenAiApiException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OpenAiRecommendationReasonClientTest {

    @Mock
    private OpenAiProxyClient openAiProxyClient;

    private OpenAiRecommendationReasonClient client;

    @BeforeEach
    void setUp() {
        client = new OpenAiRecommendationReasonClient(openAiProxyClient);
    }

    @Test
    void 정해진_형식의_응답을_추천이유와_주의사항으로_파싱한다() {
        when(openAiProxyClient.chat(any())).thenReturn(
                "추천 이유: 편의점이 가까워요, 녹지 조성이 잘 되어 있어요\n주의사항: 유흥가가 가까워요");

        GeneratedReason result = client.generate(context());

        assertThat(result.getRecommendationReason()).isEqualTo("편의점이 가까워요, 녹지 조성이 잘 되어 있어요");
        assertThat(result.getCaution()).isEqualTo("유흥가가 가까워요");
    }

    @Test
    void 프롬프트에_행정동_이름과_카테고리_점수가_포함된다() {
        when(openAiProxyClient.chat(any())).thenReturn("추천 이유: 이유\n주의사항: 주의");

        client.generate(context());

        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(openAiProxyClient).chat(promptCaptor.capture());

        assertThat(promptCaptor.getValue())
                .contains("마포구 서교동")
                .contains("안전")
                .contains("70.00");
    }

    @Test
    void 프록시_호출이_실패하면_플레이스홀더로_폴백한다() {
        when(openAiProxyClient.chat(any())).thenThrow(new OpenAiApiException("호출 실패"));

        GeneratedReason result = client.generate(context());

        assertThat(result.getRecommendationReason()).isEqualTo("추후 openai api 호출");
        assertThat(result.getCaution()).isEqualTo("추후 openai api 호출");
    }

    @Test
    void 응답이_정해진_형식이_아니면_플레이스홀더로_폴백한다() {
        when(openAiProxyClient.chat(any())).thenReturn("그냥 아무 텍스트");

        GeneratedReason result = client.generate(context());

        assertThat(result.getRecommendationReason()).isEqualTo("추후 openai api 호출");
        assertThat(result.getCaution()).isEqualTo("추후 openai api 호출");
    }

    private RecommendationReasonContext context() {
        return RecommendationReasonContext.builder()
                .adminDongId(1L)
                .adminDongName("마포구 서교동")
                .conditionId(1L)
                .totalScore(new BigDecimal("76.80"))
                .commuteTime(30)
                .transferCount(1)
                .categoryBreakdowns(List.of(
                        CategoryScoreBreakdown.builder()
                                .category("SAFETY")
                                .rawScore(new BigDecimal("70.00"))
                                .weight(new BigDecimal("1.0"))
                                .weightedScore(new BigDecimal("0.50"))
                                .build()))
                .build();
    }
}
