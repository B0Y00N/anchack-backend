package com.kbait.anchack.place.config;

import lombok.Getter;

import java.util.Objects;

@Getter
public final class KakaoPlaceApiProperties {

    private static final int MAX_ALLOWED_RADIUS = 20_000;
    private static final int MAX_ALLOWED_PAGE_SIZE = 15;
    private static final int MAX_ALLOWED_PAGE = 45;

    private final String restApiKey;
    private final int radius;
    private final int pageSize;
    private final int maxPage;
    private final int connectTimeoutMs;
    private final int readTimeoutMs;

    public KakaoPlaceApiProperties(
            String restApiKey,
            int radius,
            int pageSize,
            int maxPage,
            int connectTimeoutMs,
            int readTimeoutMs
    ) {
        this.restApiKey = Objects.requireNonNull(restApiKey, "restApiKey는 null일 수 없습니다.");
        this.radius = requireRange(radius, 1, MAX_ALLOWED_RADIUS, "radius");
        this.pageSize = requireRange(pageSize, 1, MAX_ALLOWED_PAGE_SIZE, "pageSize");
        this.maxPage = requireRange(maxPage, 1, MAX_ALLOWED_PAGE, "maxPage");
        this.connectTimeoutMs = requirePositive(connectTimeoutMs, "connectTimeoutMs");
        this.readTimeoutMs = requirePositive(readTimeoutMs, "readTimeoutMs");
    }

    private int requireRange(
            int value,
            int minimum,
            int maximum,
            String propertyName
    ) {
        if (value < minimum || value > maximum) {
            throw new IllegalArgumentException(
                    propertyName + "는 " + minimum + " 이상 " + maximum + " 이하여야 합니다."
            );
        }

        return value;
    }

    private int requirePositive(int value, String propertyName) {
        if (value < 1) {
            throw new IllegalArgumentException(propertyName + "는 1 이상이어야 합니다.");
        }

        return value;
    }
}
