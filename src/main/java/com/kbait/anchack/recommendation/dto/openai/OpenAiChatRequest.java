package com.kbait.anchack.recommendation.dto.openai;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public final class OpenAiChatRequest {

    private String model;
    private List<OpenAiChatMessage> messages;

    @JsonProperty("max_tokens")
    private int maxTokens;
}
