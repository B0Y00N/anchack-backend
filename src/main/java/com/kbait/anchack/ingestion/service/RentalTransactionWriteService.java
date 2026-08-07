package com.kbait.anchack.ingestion.service;

import com.kbait.anchack.ingestion.domain.RentalTransaction;
import com.kbait.anchack.ingestion.domain.RentalTransactionCategoryCounts;

import java.time.YearMonth;
import java.util.List;

public interface RentalTransactionWriteService {

    void replaceMonthlyTransactions(
            String guCode,
            YearMonth dealYearMonth,
            List<RentalTransaction> transactions,
            RentalTransactionCategoryCounts categoryCounts
    );
}
