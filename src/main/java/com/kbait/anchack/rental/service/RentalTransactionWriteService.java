package com.kbait.anchack.rental.service;

import com.kbait.anchack.rental.domain.RentalTransaction;
import com.kbait.anchack.rental.domain.RentalTransactionCategoryCounts;

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
