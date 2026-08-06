package com.kbait.anchack.ingestion.service;

import java.time.YearMonth;

public interface MolitRentIngestionService {

    void ingestMonthlyTransactions(
            String guCode,
            YearMonth dealYearMonth
    );
}
