package com.kbait.anchack.rental.config;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class MolitRentApiProperties {

    private final String serviceKey;
    private final int numOfRows;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public MolitRentApiProperties(
            String serviceKey,
            int numOfRows,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        this.serviceKey = Objects.requireNonNull(
                serviceKey,
                "국토부 API 서비스 키는 null일 수 없습니다."
        );
        this.numOfRows = requirePositive(numOfRows, "numOfRows");
        this.connectTimeoutMs = requirePositive(connectTimeoutMs, "connectTimeoutMs");
        this.readTimeoutMs = requirePositive(readTimeoutMs, "readTimeoutMs");
    }

    private static int requirePositive(int value, String propertyName) {
        if (value < 1) {
            throw new IllegalArgumentException(propertyName + "는 1 이상이어야 합니다.");
        }

        return value;
    }
}
