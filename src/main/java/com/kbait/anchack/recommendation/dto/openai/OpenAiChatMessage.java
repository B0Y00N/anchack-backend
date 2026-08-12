package com.kbait.anchack.recommendation.dto.openai;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class OpenAiChatMessage {

    private String role;
    private String content;
}
