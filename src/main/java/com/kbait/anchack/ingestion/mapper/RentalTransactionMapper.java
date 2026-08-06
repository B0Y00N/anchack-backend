package com.kbait.anchack.ingestion.mapper;

import com.kbait.anchack.ingestion.domain.RentalTransaction;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

public interface RentalTransactionMapper {

    int deleteByGuCodeAndTransactionDateRange(
            @Param("guCode") String guCode,
            @Param("startDate") LocalDate startDate,
            @Param("endDateExclusive") LocalDate endDateExclusive
    );

    int insertBatch(@Param("transactions") List<RentalTransaction> transactions);
}
