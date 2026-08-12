package com.kbait.anchack.route.config;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class RouteApiProperties {

    private final String restApiKey;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public RouteApiProperties(
            String restApiKey,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        this.restApiKey = Objects.requireNonNull(restApiKey, "restApiKey는 null일 수 없습니다.");
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
