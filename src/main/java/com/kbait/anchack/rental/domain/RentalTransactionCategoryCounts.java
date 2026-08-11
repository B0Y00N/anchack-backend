package com.kbait.anchack.rental.domain;

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

    public boolean canBeReplacedBy(RentalTransactionCategoryCounts replacementCounts) {
        if (replacementCounts == null) {
            throw new IllegalArgumentException("replacementCounts는 null일 수 없습니다.");
        }

        return canCategoryBeReplaced(officetelCount, replacementCounts.officetelCount)
                && canCategoryBeReplaced(rowHouseCount, replacementCounts.rowHouseCount)
                && canCategoryBeReplaced(singleHouseCount, replacementCounts.singleHouseCount);
    }

    private boolean canCategoryBeReplaced(long existingCount, long replacementCount) {
        return existingCount == 0 || replacementCount > 0;
    }

    private long requireNonNegative(long count, String fieldName) {
        if (count < 0) {
            throw new IllegalArgumentException(fieldName + "는 0 이상이어야 합니다.");
        }

        return count;
    }
}
