package com.kbait.anchack.ingestion.domain;

import lombok.Getter;

@Getter
public final class RentalTransactionCategoryCounts {

    private final long officetelCount;
    private final long rowHouseCount;
    private final long singleHouseCount;

    public RentalTransactionCategoryCounts(
            long officetelCount,
            long rowHouseCount,
            long singleHouseCount
    ) {
        this.officetelCount = requireNonNegative(officetelCount, "officetelCount");
        this.rowHouseCount = requireNonNegative(rowHouseCount, "rowHouseCount");
        this.singleHouseCount = requireNonNegative(singleHouseCount, "singleHouseCount");
    }

    public long totalCount() {
        return officetelCount + rowHouseCount + singleHouseCount;
    }

    private long requireNonNegative(long count, String fieldName) {
        if (count < 0) {
            throw new IllegalArgumentException(fieldName + "는 0 이상이어야 합니다.");
        }

        return count;
    }
}
