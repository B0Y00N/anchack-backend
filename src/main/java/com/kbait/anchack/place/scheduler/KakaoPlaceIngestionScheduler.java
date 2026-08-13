package com.kbait.anchack.place.scheduler;

import com.kbait.anchack.place.service.KakaoPlaceIngestionResult;
import com.kbait.anchack.place.service.KakaoPlaceIngestionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

public class KakaoPlaceIngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(KakaoPlaceIngestionScheduler.class);

    private final KakaoPlaceIngestionService kakaoPlaceIngestionService;

    public KakaoPlaceIngestionScheduler(KakaoPlaceIngestionService kakaoPlaceIngestionService) {
        this.kakaoPlaceIngestionService = kakaoPlaceIngestionService;
    }

    @Scheduled(
            cron = "${kakao.place.scheduler.cron:${KAKAO_PLACE_SCHEDULER_CRON:-}}",
            zone = "Asia/Seoul"
    )
    public void ingestPlaces() {
        try {
            KakaoPlaceIngestionResult result = kakaoPlaceIngestionService.ingest();
            log.info(
                    "카카오 장소 수집 완료: collectedCount={}, normalizedCount={}, skippedCount={}, affectedRowCount={}",
                    result.getCollectedCount(),
                    result.getNormalizedCount(),
                    result.getSkippedCount(),
                    result.getAffectedRowCount()
            );
        } catch (RuntimeException exception) {
            log.warn(
                    "카카오 장소 수집 실패: exceptionType={}",
                    exception.getClass().getName()
            );
        }
    }
}
