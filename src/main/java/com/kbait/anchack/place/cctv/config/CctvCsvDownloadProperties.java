package com.kbait.anchack.place.cctv.config;

import lombok.Getter;

import java.net.URI;
import java.util.Objects;

@Getter
public final class CctvCsvDownloadProperties {

    private final URI validationUri;
    private final URI downloadUri;
    private final String referer;
    private final String userAgent;
    private final int connectTimeoutMs;
    private final int requestTimeoutMs;

    public CctvCsvDownloadProperties(
            String validationUrl,
            String downloadUrl,
            String referer,
            String userAgent,
            int connectTimeoutMs,
            int requestTimeoutMs
    ) {
        this.validationUri = URI.create(requireText(validationUrl, "validationUrl"));
        this.downloadUri = URI.create(requireText(downloadUrl, "downloadUrl"));
        this.referer = requireText(referer, "referer");
        this.userAgent = requireText(userAgent, "userAgent");
        this.connectTimeoutMs = requirePositive(connectTimeoutMs, "connectTimeoutMs");
        this.requestTimeoutMs = requirePositive(requestTimeoutMs, "requestTimeoutMs");
    }

    private String requireText(String value, String fieldName) {
        String normalizedValue = Objects.requireNonNull(value, fieldName + " must not be null.").trim();
        if (normalizedValue.isEmpty()) {
            throw new IllegalArgumentException(fieldName + " must not be blank.");
        }

        return normalizedValue;
    }

    private int requirePositive(int value, String fieldName) {
        if (value < 1) {
            throw new IllegalArgumentException(fieldName + " must be positive.");
        }

        return value;
    }
}
