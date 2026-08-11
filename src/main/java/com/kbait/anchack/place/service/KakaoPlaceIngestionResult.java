package com.kbait.anchack.place.service;

import lombok.Getter;

@Getter
public final class KakaoPlaceIngestionResult {

    private final int collectedCount;
    private final int normalizedCount;
    private final int skippedCount;
    private final int affectedRowCount;

    public KakaoPlaceIngestionResult(
            int collectedCount,
            int normalizedCount,
            int skippedCount,
            int affectedRowCount
    ) {
        this.collectedCount = requireNonNegative(collectedCount, "collectedCount");
        this.normalizedCount = requireNonNegative(normalizedCount, "normalizedCount");
        this.skippedCount = requireNonNegative(skippedCount, "skippedCount");
        this.affectedRowCount = requireNonNegative(affectedRowCount, "affectedRowCount");
    }

    private int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new IllegalArgumentException(fieldName + " must not be negative.");
        }

        return value;
    }
}
