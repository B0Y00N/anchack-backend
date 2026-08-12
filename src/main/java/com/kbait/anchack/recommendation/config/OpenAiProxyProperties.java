package com.kbait.anchack.recommendation.config;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class OpenAiProxyProperties {

    private final String baseUrl;
    private final String apiToken;
    private final String model;
    private final int maxTokens;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public OpenAiProxyProperties(
            String baseUrl,
            String apiToken,
            String model,
            int maxTokens,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        this.baseUrl = Objects.requireNonNull(baseUrl, "baseUrl는 null일 수 없습니다.");
        this.apiToken = Objects.requireNonNull(apiToken, "apiToken은 null일 수 없습니다.");
        this.model = Objects.requireNonNull(model, "model은 null일 수 없습니다.");
        this.maxTokens = requirePositive(maxTokens, "maxTokens");
        this.connectTimeoutMs = requirePositive(connectTimeoutMs, "connectTimeoutMs");
        this.readTimeoutMs = requirePositive(readTimeoutMs, "readTimeoutMs");
    }

    private int requirePositive(int value, String propertyName) {
        if (value < 1) {
            throw new IllegalArgumentException(propertyName + "는 1 이상이어야 합니다.");
        }

        return value;
    }
}
