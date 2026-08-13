package com.kbait.anchack.rental.config;

import com.kbait.anchack.common.config.RootConfig;
import com.kbait.anchack.common.config.SchedulingConfig;
import com.kbait.anchack.place.config.PlaceConfig;
import com.kbait.anchack.recommendation.config.RecommendationConfig;
import com.kbait.anchack.rental.scheduler.MolitRentIngestionScheduler;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import com.kbait.anchack.route.config.RouteConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.BeanCreationException;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.mock.env.MockEnvironment;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.time.Clock;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class MolitRentSchedulerConfigTest {

    private static final String ACTIVE_CRON = "0 0 3 * * MON";

    @Test
    void 공통_SchedulingConfig에만_EnableScheduling을_선언한다() {
        assertThat(SchedulingConfig.class.isAnnotationPresent(EnableScheduling.class)).isTrue();
        assertThat(MolitRentSchedulerConfig.class.isAnnotationPresent(EnableScheduling.class)).isFalse();
        assertThat(MolitRentSchedulerConfig.class.isAnnotationPresent(Configuration.class)).isTrue();
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
    void 기본_비활성_설정으로_경량_Context를_시작하고_Scheduler_Bean을_등록한다() {
        try (AnnotationConfigApplicationContext context = createContext(new MockEnvironment())) {
            MolitRentSchedulerProperties properties = context.getBean(MolitRentSchedulerProperties.class);
            Clock clock = context.getBean("molitRentClock", Clock.class);
            MolitRentIngestionService ingestionService = context.getBean(MolitRentIngestionService.class);

            assertThat(properties.getCron()).isEqualTo("-");
            assertThat(properties.getLawdCodes()).isEmpty();
            assertThat(clock.getZone()).isEqualTo(ZoneId.of("Asia/Seoul"));
            assertThat(context.getBean(MolitRentIngestionScheduler.class)).isNotNull();
            verifyNoInteractions(ingestionService);
        }
    }

    @Test
    void 활성_cron과_빈_코드_목록이면_Context_시작에_실패한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("molit.rent.scheduler.cron", ACTIVE_CRON);

        assertThatThrownBy(() -> createContext(environment))
                .isInstanceOf(BeanCreationException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasRootCauseMessage("활성 cron에는 법정 시군구 코드가 하나 이상 필요합니다.");
    }

    @Test
    void 환경변수_이름으로_전달된_설정값을_fallback으로_사용한다() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("MOLIT_RENT_SCHEDULER_CRON", ACTIVE_CRON)
                .withProperty("MOLIT_RENT_SCHEDULER_LAWD_CODES", "11110,11140");

        try (AnnotationConfigApplicationContext context = createContext(environment)) {
            MolitRentSchedulerProperties properties = context.getBean(MolitRentSchedulerProperties.class);

            assertThat(properties.getCron()).isEqualTo(ACTIVE_CRON);
            assertThat(properties.getLawdCodes()).containsExactly("11110", "11140");
        }
    }

    @Test
    void RootConfig는_기존_Config와_Scheduling_Config를_모두_import한다() {
        Import importAnnotation = AnnotationUtils.findAnnotation(RootConfig.class, Import.class);

        assertThat(importAnnotation).isNotNull();
        assertThat(Arrays.asList(importAnnotation.value())).containsExactly(
                PlaceConfig.class,
                RecommendationConfig.class,
                MolitRentConfig.class,
                RouteConfig.class,
                SchedulingConfig.class,
                MolitRentSchedulerConfig.class
        );
    }

    @Test
    void 경량_Context에는_사용자_정의_TaskScheduler와_Executor_Bean이_없다() {
        try (AnnotationConfigApplicationContext context = createContext(new MockEnvironment())) {
            assertThat(context.getBeanNamesForType(TaskScheduler.class)).isEmpty();
            assertThat(context.getBeanNamesForType(Executor.class)).isEmpty();
        }
    }

    private AnnotationConfigApplicationContext createContext(MockEnvironment environment) {
        AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext();
        context.setEnvironment(environment);
        context.register(
                SchedulingConfig.class,
                MolitRentSchedulerConfig.class,
                TestMolitRentIngestionServiceConfig.class
        );
        context.refresh();

        return context;
    }

    @Configuration
    static class TestMolitRentIngestionServiceConfig {

        @Bean
        MolitRentIngestionService molitRentIngestionService() {
            return mock(MolitRentIngestionService.class);
        }
    }
}
