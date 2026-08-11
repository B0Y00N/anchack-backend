package com.kbait.anchack.rental.service.impl;

import com.kbait.anchack.rental.client.MolitRentApiCategory;
import com.kbait.anchack.rental.client.MolitRentApiClient;
import com.kbait.anchack.rental.domain.RentalTransaction;
import com.kbait.anchack.rental.domain.RentalTransactionCategoryCounts;
import com.kbait.anchack.rental.dto.external.RawRentalTransaction;
import com.kbait.anchack.rental.normalizer.RentalTransactionNormalizer;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import com.kbait.anchack.rental.service.RentalTransactionWriteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
            YearMonth dealYearMonth
    ) {
        validateInputs(guCode, dealYearMonth);

        List<RawRentalTransaction> officetelTransactions = molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.OFFICETEL,
                guCode,
                dealYearMonth
        );
        long officetelCount = officetelTransactions.size();
        List<RawRentalTransaction> rowHouseTransactions = molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.ROW_HOUSE,
                guCode,
                dealYearMonth
        );
        long rowHouseCount = rowHouseTransactions.size();
        List<RawRentalTransaction> singleHouseTransactions = molitRentApiClient.fetchAllPages(
                MolitRentApiCategory.SINGLE_HOUSE,
                guCode,
                dealYearMonth
        );
        long singleHouseCount = singleHouseTransactions.size();

        List<RawRentalTransaction> rawTransactions = combineTransactions(
                officetelTransactions,
                rowHouseTransactions,
                singleHouseTransactions
        );
        RentalTransactionCategoryCounts categoryCounts = new RentalTransactionCategoryCounts(
                officetelCount,
                rowHouseCount,
                singleHouseCount
        );
        List<RentalTransaction> normalizedTransactions = normalizeTransactions(rawTransactions);
        rentalTransactionWriteService.replaceMonthlyTransactions(
                guCode,
                dealYearMonth,
                normalizedTransactions,
                categoryCounts
        );
    }

    private List<RawRentalTransaction> combineTransactions(
            List<RawRentalTransaction> officetelTransactions,
            List<RawRentalTransaction> rowHouseTransactions,
            List<RawRentalTransaction> singleHouseTransactions
    ) {
        List<RawRentalTransaction> rawTransactions = new ArrayList<>();
        rawTransactions.addAll(officetelTransactions);
        rawTransactions.addAll(rowHouseTransactions);
        rawTransactions.addAll(singleHouseTransactions);
        return rawTransactions;
    }

    private List<RentalTransaction> normalizeTransactions(List<RawRentalTransaction> rawTransactions) {
        List<RentalTransaction> normalizedTransactions = new ArrayList<>(rawTransactions.size());
        for (RawRentalTransaction rawTransaction : rawTransactions) {
            normalizedTransactions.add(rentalTransactionNormalizer.normalize(rawTransaction));
        }
        return normalizedTransactions;
    }

    private void validateInputs(
            String guCode,
            YearMonth dealYearMonth
    ) {
        if (guCode == null || !GU_CODE_PATTERN.matcher(guCode).matches()) {
            throw new IllegalArgumentException("guCode는 숫자 5자리여야 합니다.");
        }
        if (dealYearMonth == null) {
            throw new IllegalArgumentException("dealYearMonth는 null일 수 없습니다.");
        }
    }
}
