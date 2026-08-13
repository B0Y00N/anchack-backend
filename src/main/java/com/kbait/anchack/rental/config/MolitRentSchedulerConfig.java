package com.kbait.anchack.rental.config;

import com.kbait.anchack.rental.scheduler.MolitRentIngestionScheduler;
import com.kbait.anchack.rental.service.MolitRentIngestionService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class MolitRentSchedulerConfig {

    private static final ZoneId SEOUL_ZONE_ID = ZoneId.of("Asia/Seoul");

    @Bean
    public MolitRentSchedulerProperties molitRentSchedulerProperties(
            @Value("${molit.rent.scheduler.cron:${MOLIT_RENT_SCHEDULER_CRON:-}}") String cron,
            @Value("${molit.rent.scheduler.lawd-codes:${MOLIT_RENT_SCHEDULER_LAWD_CODES:}}") String lawdCodes
    ) {
        return new MolitRentSchedulerProperties(cron, lawdCodes);
    }

    @Bean(name = "molitRentClock")
    public Clock molitRentClock() {
        return Clock.system(SEOUL_ZONE_ID);
    }

    @Bean
    public MolitRentIngestionScheduler molitRentIngestionScheduler(
            MolitRentIngestionService molitRentIngestionService,
            MolitRentSchedulerProperties properties,
            @Qualifier("molitRentClock") Clock clock
    ) {
        return new MolitRentIngestionScheduler(molitRentIngestionService, properties, clock);
    }
}
