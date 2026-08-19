package com.kbait.anchack.rental.initialload;

import com.kbait.anchack.rental.config.MolitRentApiProperties;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import org.junit.jupiter.api.Test;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class MolitRentInitialLoadCliTest {

    private static final Clock AUGUST_2026_CLOCK = Clock.fixed(
            Instant.parse("2026-08-14T00:00:00Z"),
            ZoneId.of("Asia/Seoul")
    );

    @Test
    void 옵션_오류는_Context를_생성하기_전에_종료한다() {
        MolitRentInitialLoadCli.ContextFactory contextFactory = mock(MolitRentInitialLoadCli.ContextFactory.class);
        MolitRentInitialLoadCli cli = new MolitRentInitialLoadCli(contextFactory);

        assertThat(cli.run(new String[]{"--all", "--lawd-code=11620"})).isEqualTo(2);
        assertThat(cli.run(new String[]{"--year-month=2026-07"})).isEqualTo(2);
        assertThat(cli.run(new String[]{"--unknown"})).isEqualTo(2);
        assertThat(cli.run(new String[]{"--year-month=2025-08", "--lawd-code=11620"})).isEqualTo(2);
        assertThat(cli.run(new String[]{"--year-month=2026-07", "--lawd-code=26440"})).isEqualTo(2);
        verifyNoInteractions(contextFactory);
    }

    @Test
    void 부분_실행_옵션은_fixed_KST_Clock으로_범위를_검증한다() {
        assertThatThrownBy(() -> MolitRentInitialLoadCli.parseRequest(
                new String[]{"--year-month=2025-08", "--lawd-code=11620"},
                AUGUST_2026_CLOCK
        )).isInstanceOf(IllegalArgumentException.class);

        assertThat(MolitRentInitialLoadCli.parseRequest(
                new String[]{"--year-month=2025-09", "--lawd-code=11620"},
                AUGUST_2026_CLOCK
        ).getYearMonth()).hasToString("2025-09");
    }

    @Test
    void CLI_스케줄러_비활성_속성은_기존_속성보다_우선한다() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addLast(new MapPropertySource("existing", java.util.Map.of(
                "molit.rent.scheduler.cron", "0 0 3 * * MON",
                "kakao.place.scheduler.cron", "0 0 3 * * MON",
                "cctv.place.scheduler.cron", "0 0 3 * * MON"
        )));
        environment.getPropertySources().addFirst(
                new MapPropertySource("cli", MolitRentInitialLoadCli.schedulerOverrides())
        );

        assertThat(environment.getProperty("molit.rent.scheduler.cron")).isEqualTo("-");
        assertThat(environment.getProperty("kakao.place.scheduler.cron")).isEqualTo("-");
        assertThat(environment.getProperty("cctv.place.scheduler.cron")).isEqualTo("-");
        assertThat(MolitRentInitialLoadCli.schedulerOverrides())
                .doesNotContainKey("molit.rent.scheduler.lawd-codes");
    }

    @Test
    void 모든_작업이_성공하면_0으로_종료한다() {
        ConfigurableApplicationContext context = contextWith((lawdCode, yearMonth) -> { });
        MolitRentInitialLoadCli cli = new MolitRentInitialLoadCli(() -> context);

        assertThat(cli.run(new String[]{"--year-month=2026-07", "--lawd-code=11620"})).isZero();
        verify(context).close();
    }

    @Test
    void 작업_실패가_하나라도_있으면_1로_종료한다() {
        ConfigurableApplicationContext context = contextWith((lawdCode, yearMonth) -> {
            throw new IllegalStateException("FAILED");
        });
        MolitRentInitialLoadCli cli = new MolitRentInitialLoadCli(() -> context);

        assertThat(cli.run(new String[]{"--year-month=2026-07", "--lawd-code=11620"})).isEqualTo(1);
        verify(context).close();
    }

    @Test
    void Context_초기화_실패는_2로_종료한다() {
        MolitRentInitialLoadCli cli = new MolitRentInitialLoadCli(() -> {
            throw new IllegalStateException("CONTEXT_FAILURE");
        });

        assertThat(cli.run(new String[]{"--all"})).isEqualTo(2);
    }

    @Test
    void 국토부_API_키가_없으면_작업을_시작하지_않고_2로_종료한다() {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getBean(MolitRentApiProperties.class)).thenReturn(
                new MolitRentApiProperties("", 100, 5_000, 30_000)
        );
        MolitRentInitialLoadCli cli = new MolitRentInitialLoadCli(() -> context);

        assertThat(cli.run(new String[]{"--all"})).isEqualTo(2);
        verify(context).close();
        verify(context).getBean(MolitRentApiProperties.class);
        verify(context, never()).getBean(MolitRentIngestionService.class);
    }

    private ConfigurableApplicationContext contextWith(MolitRentIngestionService ingestionService) {
        ConfigurableApplicationContext context = mock(ConfigurableApplicationContext.class);
        when(context.getBean(MolitRentApiProperties.class)).thenReturn(
                new MolitRentApiProperties("TEST_SERVICE_KEY", 100, 5_000, 30_000)
        );
        when(context.getBean(MolitRentIngestionService.class)).thenReturn(ingestionService);
        when(context.getBean("molitRentClock", Clock.class)).thenReturn(AUGUST_2026_CLOCK);
        return context;
    }
}
