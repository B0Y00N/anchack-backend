package com.kbait.anchack.rental.initialload;

import com.kbait.anchack.rental.service.MolitRentIngestionService;

import java.time.Clock;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * 명시적으로 실행한 국토부 전월세 초기 적재만 담당한다.
 * 기존 주간 스케줄러와 별개의 일반 객체이므로 Spring Bean으로 등록하지 않는다.
 */
public final class MolitRentInitialLoadRunner {

    public static final List<String> SEOUL_LAWD_CODES = List.of(
            "11110", "11140", "11170", "11200", "11215",
            "11230", "11260", "11290", "11305", "11320",
            "11350", "11380", "11410", "11440", "11470",
            "11500", "11530", "11545", "11560", "11590",
            "11620", "11650", "11680", "11710", "11740"
    );

    private final MolitRentIngestionService molitRentIngestionService;
    private final Clock clock;
    private final List<String> lawdCodes;

    public MolitRentInitialLoadRunner(
            MolitRentIngestionService molitRentIngestionService,
            Clock clock
    ) {
        this(molitRentIngestionService, clock, SEOUL_LAWD_CODES);
    }

    MolitRentInitialLoadRunner(
            MolitRentIngestionService molitRentIngestionService,
            Clock clock,
            List<String> lawdCodes
    ) {
        this.molitRentIngestionService = Objects.requireNonNull(
                molitRentIngestionService,
                "molitRentIngestionService은 null일 수 없습니다."
        );
        this.clock = Objects.requireNonNull(clock, "clock은 null일 수 없습니다.");
        this.lawdCodes = List.copyOf(Objects.requireNonNull(lawdCodes, "lawdCodes는 null일 수 없습니다."));

        if (this.lawdCodes.isEmpty()) {
            throw new IllegalArgumentException("초기 적재 대상 구 코드는 하나 이상이어야 합니다.");
        }
    }

    public InitialLoadResult run(InitialLoadRequest request) {
        Objects.requireNonNull(request, "request는 null일 수 없습니다.");

        List<YearMonth> targetYearMonths = createTargetYearMonths(request);
        List<String> targetLawdCodes = createTargetLawdCodes(request);
        List<FailedTask> failedTasks = new ArrayList<>();
        int successfulTaskCount = 0;

        for (YearMonth dealYearMonth : targetYearMonths) {
            for (String lawdCode : targetLawdCodes) {
                try {
                    molitRentIngestionService.ingestMonthlyTransactions(lawdCode, dealYearMonth);
                    successfulTaskCount++;
                } catch (RuntimeException exception) {
                    failedTasks.add(new FailedTask(lawdCode, dealYearMonth, exception.getClass().getName()));
                }
            }
        }

        return new InitialLoadResult(successfulTaskCount, failedTasks);
    }

    private List<YearMonth> createTargetYearMonths(InitialLoadRequest request) {
        YearMonth currentYearMonth = YearMonth.now(clock);
        YearMonth oldestYearMonth = currentYearMonth.minusMonths(11);

        if (request.isAll()) {
            List<YearMonth> targetYearMonths = new ArrayList<>(12);
            for (int offset = 0; offset < 12; offset++) {
                targetYearMonths.add(oldestYearMonth.plusMonths(offset));
            }
            return List.copyOf(targetYearMonths);
        }

        YearMonth requestedYearMonth = request.getYearMonth();
        if (requestedYearMonth.isBefore(oldestYearMonth) || requestedYearMonth.isAfter(currentYearMonth)) {
            throw new IllegalArgumentException(
                    "초기 적재 대상 월은 KST 기준 현재월과 직전 11개월 범위여야 합니다."
            );
        }

        return List.of(requestedYearMonth);
    }

    private List<String> createTargetLawdCodes(InitialLoadRequest request) {
        if (request.isAll()) {
            return lawdCodes;
        }

        String requestedLawdCode = request.getLawdCode();
        if (!lawdCodes.contains(requestedLawdCode)) {
            throw new IllegalArgumentException("초기 적재 대상은 서울 25개 구 법정 시군구 코드여야 합니다.");
        }

        return List.of(requestedLawdCode);
    }

    public static final class InitialLoadRequest {

        private final boolean all;
        private final YearMonth yearMonth;
        private final String lawdCode;

        private InitialLoadRequest(boolean all, YearMonth yearMonth, String lawdCode) {
            this.all = all;
            this.yearMonth = yearMonth;
            this.lawdCode = lawdCode;
        }

        public static InitialLoadRequest all() {
            return new InitialLoadRequest(true, null, null);
        }

        public static InitialLoadRequest partial(YearMonth yearMonth, String lawdCode) {
            return new InitialLoadRequest(
                    false,
                    Objects.requireNonNull(yearMonth, "yearMonth는 null일 수 없습니다."),
                    Objects.requireNonNull(lawdCode, "lawdCode는 null일 수 없습니다.")
            );
        }

        public boolean isAll() {
            return all;
        }

        public YearMonth getYearMonth() {
            return yearMonth;
        }

        public String getLawdCode() {
            return lawdCode;
        }
    }

    public static final class InitialLoadResult {

        private final int successfulTaskCount;
        private final List<FailedTask> failedTasks;

        private InitialLoadResult(int successfulTaskCount, List<FailedTask> failedTasks) {
            this.successfulTaskCount = successfulTaskCount;
            this.failedTasks = List.copyOf(failedTasks);
        }

        public int getSuccessfulTaskCount() {
            return successfulTaskCount;
        }

        public int getFailedTaskCount() {
            return failedTasks.size();
        }

        public List<FailedTask> getFailedTasks() {
            return failedTasks;
        }

        public boolean hasFailures() {
            return !failedTasks.isEmpty();
        }
    }

    public static final class FailedTask {

        private final String lawdCode;
        private final YearMonth yearMonth;
        private final String exceptionType;

        private FailedTask(String lawdCode, YearMonth yearMonth, String exceptionType) {
            this.lawdCode = lawdCode;
            this.yearMonth = yearMonth;
            this.exceptionType = exceptionType;
        }

        public String getLawdCode() {
            return lawdCode;
        }

        public YearMonth getYearMonth() {
            return yearMonth;
        }

        public String getExceptionType() {
            return exceptionType;
        }
    }
}
