package com.kbait.anchack.place.scheduler;

import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionResult;
import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class CctvPlaceIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(CctvPlaceIngestionScheduler.class);

    private final CctvPlaceIngestionRunner cctvPlaceIngestionRunner;

    public CctvPlaceIngestionScheduler(CctvPlaceIngestionRunner cctvPlaceIngestionRunner) {
        this.cctvPlaceIngestionRunner = cctvPlaceIngestionRunner;
    }

    @Scheduled(
            cron = "${cctv.place.scheduler.cron:${CCTV_PLACE_SCHEDULER_CRON:-}}",
            zone = "Asia/Seoul"
    )
    public void ingestPlaces() {
        try {
            CctvPlaceIngestionResult result = cctvPlaceIngestionRunner.run();
            log.info(
                    "CCTV 장소 수집 완료: collectedCount={}, normalizedCount={}, skippedCount={}, affectedRowCount={}",
                    result.getCollectedCount(),
                    result.getNormalizedCount(),
                    result.getSkippedCount(),
                    result.getAffectedRowCount()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "CCTV 장소 수집 실패: exceptionType={}",
                    exception.getClass().getName()
            );
        }
    }
}
