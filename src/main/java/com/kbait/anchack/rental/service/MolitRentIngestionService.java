package com.kbait.anchack.rental.service;

import java.time.YearMonth;

public interface MolitRentIngestionService {

    void ingestMonthlyTransactions(
            String guCode,
            YearMonth dealYearMonth
    );

    default IngestionExecution openExecution() {
        return new IngestionExecution() {

            private boolean closed;

            @Override
            public void ingestMonthlyTransactions(String guCode, YearMonth dealYearMonth) {
                if (closed) {
                    throw new IllegalStateException("이미 닫힌 전월세 수집 execution입니다.");
                }
                MolitRentIngestionService.this.ingestMonthlyTransactions(guCode, dealYearMonth);
            }

            @Override
            public void close() {
                closed = true;
            }
        };
    }

    interface IngestionExecution extends AutoCloseable {

        void ingestMonthlyTransactions(String guCode, YearMonth dealYearMonth);

        @Override
        void close();
    }
}
