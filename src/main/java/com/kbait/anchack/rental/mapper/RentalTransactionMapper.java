package com.kbait.anchack.rental.mapper;

import com.kbait.anchack.admindong.dto.RentalAmountRow;
import com.kbait.anchack.rental.domain.RentalTransaction;
import com.kbait.anchack.rental.domain.RentalTransactionCategoryCounts;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentalTransactionMapper {

    RentalTransactionCategoryCounts findCategoryCountsByGuCodeAndTransactionDateRange(
            @Param("guCode") String guCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive
    );

    /** 행정동 상세(P1-b)의 보증금/월세 중위값·구간별 분포 계산용 원본 거래 목록. */
    List<RentalAmountRow> findAmountsByAdminDongIdsAndRentalType(
            @Param("adminDongIds") List<Long> adminDongIds,
            @Param("rentalType") String rentalType
    );

    int deleteByGuCodeAndTransactionDateRange(
            @Param("guCode") String guCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive
    );

    int insertBatch(@Param("transactions") List<RentalTransaction> transactions);
}
