package com.kbait.anchack.rental.service.impl;

import com.kbait.anchack.rental.client.MolitRentApiCategory;
import com.kbait.anchack.rental.client.MolitRentApiClient;
import com.kbait.anchack.rental.domain.RentalTransaction;
import com.kbait.anchack.rental.domain.RentalTransactionCategoryCounts;
import com.kbait.anchack.rental.dto.external.RawRentalTransaction;
import com.kbait.anchack.rental.normalizer.RentalTransactionNormalizer;
import com.kbait.anchack.rental.registry.SeoulLawdCodeRegistry;
import com.kbait.anchack.rental.resolver.RentalAdminDongResolution;
import com.kbait.anchack.rental.resolver.RentalAdminDongResolution.Status;
import com.kbait.anchack.rental.resolver.RentalAdminDongResolver;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import com.kbait.anchack.rental.service.RentalTransactionWriteService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MolitRentIngestionServiceImpl implements MolitRentIngestionService {

    private static final Logger log = LoggerFactory.getLogger(MolitRentIngestionServiceImpl.class);
    private static final Pattern GU_CODE_PATTERN = Pattern.compile("[0-9]{5}");

    private final MolitRentApiClient molitRentApiClient;
    private final RentalTransactionNormalizer rentalTransactionNormalizer;
    private final RentalTransactionWriteService rentalTransactionWriteService;
    private final RentalAdminDongResolver rentalAdminDongResolver;

    @Override
    public void ingestMonthlyTransactions(
            String guCode,
            YearMonth dealYearMonth
    ) {
        try (IngestionExecution execution = openExecution()) {
            execution.ingestMonthlyTransactions(guCode, dealYearMonth);
        }
    }

    @Override
    public IngestionExecution openExecution() {
        return new IngestionExecutionImpl();
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

    private ResolutionResult resolveAndNormalizeTransactions(
            List<RawRentalTransaction> rawTransactions,
            RentalAdminDongResolver.ResolutionSession resolutionSession
    ) {
        List<RentalTransaction> normalizedTransactions = new ArrayList<>(rawTransactions.size());
        ResolutionResult result = new ResolutionResult(normalizedTransactions);

        for (RawRentalTransaction rawTransaction : rawTransactions) {
            RentalAdminDongResolution resolution = resolutionSession.resolve(rawTransaction);
            result.increment(resolution.getStatus());
            normalizedTransactions.add(rentalTransactionNormalizer.normalize(
                    rawTransaction,
                    resolution.getAdminDongId()
            ));
        }
        return result;
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
        if (!SeoulLawdCodeRegistry.seoulLawdCodes().contains(guCode)) {
            throw new IllegalArgumentException("지원하지 않는 서울 법정 시군구 코드입니다.");
        }
    }

    private final class IngestionExecutionImpl implements IngestionExecution {

        private RentalAdminDongResolver.ResolutionSession resolutionSession;
        private boolean closed;

        @Override
        public void ingestMonthlyTransactions(String guCode, YearMonth dealYearMonth) {
            ensureOpen();
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

            ResolutionResult resolutionResult;
            if (rawTransactions.isEmpty()) {
                resolutionResult = new ResolutionResult(new ArrayList<>());
            } else {
                resolutionResult = resolveAndNormalizeTransactions(
                        rawTransactions,
                        getOrOpenResolutionSession()
                );
            }
            rentalTransactionWriteService.replaceMonthlyTransactions(
                    guCode,
                    dealYearMonth,
                    resolutionResult.transactions,
                    categoryCounts
            );
            log.info(
                    "국토부 전월세 행정동 매핑 완료: lawdCode={}, yearMonth={}, totalCount={}, "
                            + "mappedCount={}, jibunMissingCount={}, addressNotFoundCount={}, "
                            + "boundaryNotFoundCount={}, adminDongNotFoundCount={}",
                    guCode,
                    dealYearMonth,
                    rawTransactions.size(),
                    resolutionResult.mappedCount,
                    resolutionResult.jibunMissingCount,
                    resolutionResult.addressNotFoundCount,
                    resolutionResult.boundaryNotFoundCount,
                    resolutionResult.adminDongNotFoundCount
            );
        }

        @Override
        public void close() {
            if (closed) {
                return;
            }
            closed = true;
            if (resolutionSession != null) {
                resolutionSession.close();
            }
        }

        private RentalAdminDongResolver.ResolutionSession getOrOpenResolutionSession() {
            if (resolutionSession == null) {
                resolutionSession = rentalAdminDongResolver.openSession();
            }
            return resolutionSession;
        }

        private void ensureOpen() {
            if (closed) {
                throw new IllegalStateException("이미 닫힌 전월세 수집 execution입니다.");
            }
        }
    }

    private static final class ResolutionResult {

        private final List<RentalTransaction> transactions;
        private long mappedCount;
        private long jibunMissingCount;
        private long addressNotFoundCount;
        private long boundaryNotFoundCount;
        private long adminDongNotFoundCount;

        private ResolutionResult(List<RentalTransaction> transactions) {
            this.transactions = transactions;
        }

        private void increment(Status status) {
            switch (status) {
                case MAPPED -> mappedCount++;
                case JIBUN_MISSING -> jibunMissingCount++;
                case ADDRESS_NOT_FOUND -> addressNotFoundCount++;
                case BOUNDARY_NOT_FOUND -> boundaryNotFoundCount++;
                case ADMIN_DONG_NOT_FOUND -> adminDongNotFoundCount++;
            }
        }
    }
}
