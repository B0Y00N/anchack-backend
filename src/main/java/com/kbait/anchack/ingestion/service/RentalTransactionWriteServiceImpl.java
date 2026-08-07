package com.kbait.anchack.ingestion.service;

import com.kbait.anchack.ingestion.domain.RentalTransaction;
import com.kbait.anchack.ingestion.mapper.RentalTransactionMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class RentalTransactionWriteServiceImpl implements RentalTransactionWriteService {

    private static final Pattern GU_CODE_PATTERN = Pattern.compile("[0-9]{5}");
    private static final int INSERT_CHUNK_SIZE = 500;

    private final RentalTransactionMapper rentalTransactionMapper;

    @Override
    @Transactional
    public void replaceMonthlyTransactions(
            String guCode,
            YearMonth dealYearMonth,
            List<RentalTransaction> transactions
    ) {
        validateInputs(guCode, dealYearMonth, transactions);

        LocalDate startDate = dealYearMonth.atDay(1);
        LocalDate endDateExclusive = dealYearMonth.plusMonths(1).atDay(1);
        rentalTransactionMapper.deleteByGuCodeAndTransactionDateRange(
                guCode,
                startDate,
                endDateExclusive
        );
        insertInChunks(transactions);
    }

    private void insertInChunks(List<RentalTransaction> transactions) {
        for (int startIndex = 0; startIndex < transactions.size(); startIndex += INSERT_CHUNK_SIZE) {
            int endIndex = Math.min(startIndex + INSERT_CHUNK_SIZE, transactions.size());
            rentalTransactionMapper.insertBatch(transactions.subList(startIndex, endIndex));
        }
    }

    private void validateInputs(
            String guCode,
            YearMonth dealYearMonth,
            List<RentalTransaction> transactions
    ) {
        if (guCode == null || !GU_CODE_PATTERN.matcher(guCode).matches()) {
            throw new IllegalArgumentException("guCode는 숫자 5자리여야 합니다.");
        }
        if (dealYearMonth == null) {
            throw new IllegalArgumentException("dealYearMonth는 null일 수 없습니다.");
        }
        if (transactions == null) {
            throw new IllegalArgumentException("transactions는 null일 수 없습니다.");
        }
    }
}
