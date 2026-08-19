package com.kbait.anchack.rental.initialload;

import com.kbait.anchack.rental.initialload.MolitRentInitialLoadRunner.InitialLoadRequest;
import com.kbait.anchack.rental.initialload.MolitRentInitialLoadRunner.InitialLoadResult;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import com.kbait.anchack.rental.service.MolitRentIngestionService.IngestionExecution;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class MolitRentInitialLoadRunnerTest {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");
    private static final Clock AUGUST_2026_CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T00:00:00Z"),
            SEOUL_ZONE_ID
    );

    @Test
    void KST_현재월과_직전_11개월을_오래된_월부터_구_코드_순서로_단일_스레드_실행한다() {
        List<Invocation> invocations = new ArrayList<>();
        MolitRentIngestionService service = (lawdCode, yearMonth) ->
                invocations.add(new Invocation(lawdCode, yearMonth, Thread.currentThread().getId()));
        MolitRentInitialLoadRunner runner = new MolitRentInitialLoadRunner(service, AUGUST_2026_CLOCK);

        InitialLoadResult result = runner.run(InitialLoadRequest.all());

        assertThat(result.getSuccessfulTaskCount()).isEqualTo(300);
        assertThat(result.getFailedTaskCount()).isZero();
        assertThat(invocations).hasSize(300);
        assertThat(invocations.get(0)).isEqualTo(new Invocation("11110", YearMonth.of(2025, 9), Thread.currentThread().getId()));
        assertThat(invocations.get(24)).isEqualTo(new Invocation("11740", YearMonth.of(2025, 9), Thread.currentThread().getId()));
        assertThat(invocations.get(25)).isEqualTo(new Invocation("11110", YearMonth.of(2025, 10), Thread.currentThread().getId()));
        assertThat(invocations.get(299)).isEqualTo(new Invocation("11740", YearMonth.of(2026, 8), Thread.currentThread().getId()));
        assertThat(invocations).extracting(Invocation::threadId).containsOnly(Thread.currentThread().getId());
    }

    @Test
    void 특정_월과_구만_실행한다() {
        List<Invocation> invocations = new ArrayList<>();
        MolitRentIngestionService service = (lawdCode, yearMonth) ->
                invocations.add(new Invocation(lawdCode, yearMonth, Thread.currentThread().getId()));
        MolitRentInitialLoadRunner runner = new MolitRentInitialLoadRunner(service, AUGUST_2026_CLOCK);

        InitialLoadResult result = runner.run(InitialLoadRequest.partial(YearMonth.of(2026, 7), "11620"));

        assertThat(result.getSuccessfulTaskCount()).isEqualTo(1);
        assertThat(result.getFailedTaskCount()).isZero();
        assertThat(invocations).containsExactly(new Invocation("11620", YearMonth.of(2026, 7), Thread.currentThread().getId()));
    }

    @Test
    void 중간_실패_후에도_다음_작업을_계속하고_실패_대상을_집계한다() {
        List<Invocation> invocations = new ArrayList<>();
        MolitRentIngestionService service = (lawdCode, yearMonth) -> {
            invocations.add(new Invocation(lawdCode, yearMonth, Thread.currentThread().getId()));
            if (lawdCode.equals("11140") && yearMonth.equals(YearMonth.of(2025, 9))) {
                throw new IllegalStateException("FAILED");
            }
        };
        MolitRentInitialLoadRunner runner = new MolitRentInitialLoadRunner(
                service,
                AUGUST_2026_CLOCK,
                List.of("11110", "11140")
        );

        InitialLoadResult result = runner.run(InitialLoadRequest.all());

        assertThat(invocations).hasSize(24);
        assertThat(invocations.get(2)).isEqualTo(new Invocation("11110", YearMonth.of(2025, 10), Thread.currentThread().getId()));
        assertThat(result.getSuccessfulTaskCount()).isEqualTo(23);
        assertThat(result.getFailedTaskCount()).isEqualTo(1);
        assertThat(result.getFailedTasks()).singleElement().satisfies(failedTask -> {
            assertThat(failedTask.getLawdCode()).isEqualTo("11140");
            assertThat(failedTask.getYearMonth()).isEqualTo(YearMonth.of(2025, 9));
            assertThat(failedTask.getExceptionType()).isEqualTo(IllegalStateException.class.getName());
        });
    }

    @Test
    void 한_논리_실행에서_execution을_한_번_열고_작업_실패_후에도_계속한_뒤_닫는다() {
        MolitRentIngestionService service = mock(MolitRentIngestionService.class);
        IngestionExecution execution = mock(IngestionExecution.class);
        when(service.openExecution()).thenReturn(execution);
        org.mockito.Mockito.doThrow(new IllegalStateException("FAILED"))
                .when(execution)
                .ingestMonthlyTransactions("11140", YearMonth.of(2025, 9));
        MolitRentInitialLoadRunner runner = new MolitRentInitialLoadRunner(
                service,
                AUGUST_2026_CLOCK,
                List.of("11110", "11140")
        );

        InitialLoadResult result = runner.run(InitialLoadRequest.all());

        assertThat(result.getSuccessfulTaskCount()).isEqualTo(23);
        assertThat(result.getFailedTaskCount()).isEqualTo(1);
        verify(service).openExecution();
        verify(execution).ingestMonthlyTransactions("11140", YearMonth.of(2025, 9));
        verify(execution).ingestMonthlyTransactions("11110", YearMonth.of(2025, 10));
        verify(execution).ingestMonthlyTransactions("11140", YearMonth.of(2026, 8));
        verify(execution).close();
    }

    @Test
    void 부분_실행_월이_12개월_범위를_벗어나면_서비스를_호출하지_않는다() {
        MolitRentIngestionService service = (lawdCode, yearMonth) -> {
            throw new AssertionError("서비스를 호출하면 안 됩니다.");
        };
        MolitRentInitialLoadRunner runner = new MolitRentInitialLoadRunner(service, AUGUST_2026_CLOCK);

        assertThatThrownBy(() -> runner.run(InitialLoadRequest.partial(YearMonth.of(2025, 8), "11620")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("직전 11개월");
    }

    @Test
    void 부분_실행_구가_서울_대상이_아니면_서비스를_호출하지_않는다() {
        MolitRentIngestionService service = (lawdCode, yearMonth) -> {
            throw new AssertionError("서비스를 호출하면 안 됩니다.");
        };
        MolitRentInitialLoadRunner runner = new MolitRentInitialLoadRunner(service, AUGUST_2026_CLOCK);

        assertThatThrownBy(() -> runner.run(InitialLoadRequest.partial(YearMonth.of(2026, 8), "26440")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("서울 25개 구");
    }

    private record Invocation(String lawdCode, YearMonth yearMonth, long threadId) {
    }
}
