package com.kbait.anchack.rental.scheduler;

import com.kbait.anchack.rental.config.MolitRentSchedulerProperties;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import com.kbait.anchack.rental.service.MolitRentIngestionService.IngestionExecution;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.YearMonth;
import java.util.List;

public class MolitRentIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(MolitRentIngestionScheduler.class);

    private final MolitRentIngestionService molitRentIngestionService;
    private final MolitRentSchedulerProperties properties;
    private final Clock clock;

    public MolitRentIngestionScheduler(
            MolitRentIngestionService molitRentIngestionService,
            MolitRentSchedulerProperties properties,
            @Qualifier("molitRentClock") Clock clock
    ) {
        this.molitRentIngestionService = molitRentIngestionService;
        this.properties = properties;
        this.clock = clock;
    }

    @Scheduled(
            cron = "${molit.rent.scheduler.cron:${MOLIT_RENT_SCHEDULER_CRON:-}}",
            zone = "Asia/Seoul"
    )
    public void ingestRecentMonthlyTransactions() {
        int successfulTaskCount = 0;
        int failedTaskCount = 0;

        try (IngestionExecution execution = molitRentIngestionService.openExecution()) {
            for (YearMonth dealYearMonth : getTargetYearMonths()) {
                for (String lawdCode : properties.getLawdCodes()) {
                    try {
                        execution.ingestMonthlyTransactions(lawdCode, dealYearMonth);
                        successfulTaskCount++;
                    } catch (RuntimeException exception) {
                        failedTaskCount++;
                        log.warn(
                                "국토부 전월세 수집 실패: lawdCode={}, yearMonth={}, exceptionType={}",
                                lawdCode,
                                dealYearMonth,
                                exception.getClass().getName()
                        );
                    }
                }
            }
        }

        log.info(
                "국토부 전월세 수집 완료: successfulTaskCount={}, failedTaskCount={}",
                successfulTaskCount,
                failedTaskCount
        );
    }

    private List<YearMonth> getTargetYearMonths() {
        YearMonth currentYearMonth = YearMonth.now(clock);

        return List.of(
                currentYearMonth.minusMonths(2),
                currentYearMonth.minusMonths(1),
                currentYearMonth
        );
    }
}
