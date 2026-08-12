package com.kbait.anchack.recommendation.dto.openai;

import lombok.Getter;

import java.util.List;

@Getter
public final class OpenAiChatResponse {

    private List<OpenAiChatChoice> choices = List.of();
}
