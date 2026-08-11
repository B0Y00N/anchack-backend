package com.kbait.anchack.rental.service;

import java.time.YearMonth;

public interface MolitRentIngestionService {

    void ingestMonthlyTransactions(
            String guCode,
            YearMonth dealYearMonth
    );
}
