package com.kbait.anchack.rental.scheduler;

import com.kbait.anchack.rental.config.MolitRentSchedulerProperties;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import com.kbait.anchack.rental.service.MolitRentIngestionService.IngestionExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MolitRentIngestionSchedulerTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock KST_MONTH_BOUNDARY_CLOCK = Clock.fixed(
            Instant.parse("2026-05-31T15:30:00Z"),
            SEOUL_ZONE_ID
    );

    @Mock
    private MolitRentIngestionService molitRentIngestionService;

    @Mock
    private IngestionExecution ingestionExecution;

    private MolitRentIngestionScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(molitRentIngestionService.openExecution()).thenReturn(ingestionExecution);
        scheduler = createScheduler("11110,11140");
    }

    @Test
    void KST_월_경계에서는_4월부터_6월까지_월_우선으로_순차_수집한다() {
        scheduler.ingestRecentMonthlyTransactions();

        InOrder inOrder = inOrder(molitRentIngestionService, ingestionExecution);
        inOrder.verify(molitRentIngestionService).openExecution();
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11110", YearMonth.of(2026, 4));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 4));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11110", YearMonth.of(2026, 5));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 5));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11110", YearMonth.of(2026, 6));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 6));
        inOrder.verify(ingestionExecution).close();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void 예약_메서드는_국토부_전월세_cron과_서울_시간대를_사용한다()
            throws NoSuchMethodException {
        Scheduled scheduled = MolitRentIngestionScheduler.class
                .getMethod("ingestRecentMonthlyTransactions")
                .getAnnotation(Scheduled.class);

        assertThat(scheduled.cron())
                .isEqualTo("${molit.rent.scheduler.cron:${MOLIT_RENT_SCHEDULER_CRON:-}}");
        assertThat(scheduled.zone()).isEqualTo("Asia/Seoul");
    }

    @Test
    void 중간_RuntimeException이_발생해도_다음_코드와_월을_계속_수집한다() {
        RuntimeException exception = new IllegalStateException("수집 실패");
        doThrow(exception).when(ingestionExecution)
                .ingestMonthlyTransactions("11140", YearMonth.of(2026, 4));

        scheduler.ingestRecentMonthlyTransactions();

        InOrder inOrder = inOrder(molitRentIngestionService, ingestionExecution);
        inOrder.verify(molitRentIngestionService).openExecution();
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11110", YearMonth.of(2026, 4));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 4));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11110", YearMonth.of(2026, 5));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 5));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11110", YearMonth.of(2026, 6));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 6));
        inOrder.verify(ingestionExecution).close();
        inOrder.verifyNoMoreInteractions();
        verify(ingestionExecution, times(1))
                .ingestMonthlyTransactions("11140", YearMonth.of(2026, 4));
    }

    @Test
    void 중간_Error는_전파하고_이후_코드와_월을_수집하지_않는다() {
        Error error = new AssertionError("수집 중단");
        doThrow(error).when(ingestionExecution)
                .ingestMonthlyTransactions("11140", YearMonth.of(2026, 4));

        assertThatThrownBy(() -> scheduler.ingestRecentMonthlyTransactions())
                .isSameAs(error);

        InOrder inOrder = inOrder(molitRentIngestionService, ingestionExecution);
        inOrder.verify(molitRentIngestionService).openExecution();
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11110", YearMonth.of(2026, 4));
        inOrder.verify(ingestionExecution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 4));
        inOrder.verify(ingestionExecution).close();
        inOrder.verifyNoMoreInteractions();
    }

    private MolitRentIngestionScheduler createScheduler(String lawdCodes) {
        return new MolitRentIngestionScheduler(
                molitRentIngestionService,
                new MolitRentSchedulerProperties("-", lawdCodes),
                KST_MONTH_BOUNDARY_CLOCK
        );
    }
}
