package com.kbait.anchack.rental.dto.external;

import lombok.Getter;

import java.util.List;
import java.util.Objects;

@Getter
public final class MolitRentPage {

    private final List<RawRentalTransaction> items;
    private final int pageNo;
    private final int numOfRows;
    private final int totalCount;

    public MolitRentPage(
            List<RawRentalTransaction> items,
            int pageNo,
            int numOfRows,
            int totalCount
    ) {
        this.items = List.copyOf(Objects.requireNonNull(items, "items는 null일 수 없습니다."));
        this.pageNo = pageNo;
        this.numOfRows = numOfRows;
        this.totalCount = totalCount;
    }
}
