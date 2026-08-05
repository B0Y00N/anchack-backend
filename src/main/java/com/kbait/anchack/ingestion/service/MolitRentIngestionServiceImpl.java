package com.kbait.anchack.ingestion.service;

import com.kbait.anchack.ingestion.client.MolitRentApiCategory;
import com.kbait.anchack.ingestion.client.MolitRentApiClient;
import com.kbait.anchack.ingestion.domain.RentalTransaction;
import com.kbait.anchack.ingestion.dto.external.RawRentalTransaction;
import com.kbait.anchack.ingestion.normalizer.RentalTransactionNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MolitRentIngestionServiceImpl implements MolitRentIngestionService {

    private static final Pattern GU_CODE_PATTERN = Pattern.compile("[0-9]{5}");

    private final MolitRentApiClient molitRentApiClient;
    private final RentalTransactionNormalizer rentalTransactionNormalizer;
    private final RentalTransactionWriteService rentalTransactionWriteService;

    @Override
    public void ingestMonthlyTransactions(
            String guCode,
            YearMonth dealYearMonth,
            LocalDate dataDate
    ) {
        validateInputs(guCode, dealYearMonth, dataDate);

        List<RawRentalTransaction> rawTransactions = collectAllTransactions(guCode, dealYearMonth);
        List<RentalTransaction> normalizedTransactions = normalizeTransactions(rawTransactions, dataDate);
        rentalTransactionWriteService.replaceMonthlyTransactions(
                guCode,
                dealYearMonth,
                normalizedTransactions
        );
    }

    private List<RawRentalTransaction> collectAllTransactions(
            String guCode,
            YearMonth dealYearMonth
    ) {
        List<RawRentalTransaction> rawTransactions = new ArrayList<>();
        for (MolitRentApiCategory apiCategory : MolitRentApiCategory.values()) {
            rawTransactions.addAll(molitRentApiClient.fetchAllPages(apiCategory, guCode, dealYearMonth));
        }
        return rawTransactions;
    }

    private List<RentalTransaction> normalizeTransactions(
            List<RawRentalTransaction> rawTransactions,
            LocalDate dataDate
    ) {
        List<RentalTransaction> normalizedTransactions = new ArrayList<>(rawTransactions.size());
        for (RawRentalTransaction rawTransaction : rawTransactions) {
            normalizedTransactions.add(rentalTransactionNormalizer.normalize(rawTransaction, dataDate));
        }
        return normalizedTransactions;
    }

    private void validateInputs(
            String guCode,
            YearMonth dealYearMonth,
            LocalDate dataDate
    ) {
        if (guCode == null || !GU_CODE_PATTERN.matcher(guCode).matches()) {
            throw new IllegalArgumentException("guCode는 숫자 5자리여야 합니다.");
        }
        if (dealYearMonth == null) {
            throw new IllegalArgumentException("dealYearMonth는 null일 수 없습니다.");
        }
        if (dataDate == null) {
            throw new IllegalArgumentException("dataDate는 null일 수 없습니다.");
        }
    }
}
