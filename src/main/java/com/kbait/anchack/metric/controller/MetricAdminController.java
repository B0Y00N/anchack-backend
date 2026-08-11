package com.kbait.anchack.metric.controller;

import com.kbait.anchack.common.response.ApiResponse;
import com.kbait.anchack.metric.service.CultureMetricScoreService;
import com.kbait.anchack.metric.service.FoodMetricScoreService;
import com.kbait.anchack.metric.service.HealthcareMetricScoreService;
import com.kbait.anchack.metric.service.LifeConvenienceMetricScoreService;
import com.kbait.anchack.metric.service.NatureMetricScoreService;
import com.kbait.anchack.metric.service.SafetyMetricScoreService;
import com.kbait.anchack.metric.service.SportsMetricScoreService;
import com.kbait.anchack.metric.service.TransitMetricScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * places 기반 정적 지표 재계산을 수동으로 트리거하는 개발용 관리 엔드포인트.
 * 운영에서는 스케줄러가 각 서비스를 개별 호출할 예정이라, 이 컨트롤러는 로컬 데이터
 * 채우기/재계산 용도로만 쓴다(silence는 places 기반이 아니라 여기 포함 안 함).
 *
 * 지표 테이블은 시계열 누적 구조라 재계산해도 기존 행을 지우지 않고 새 행만 INSERT한다.
 * 조회 시엔 항상 admin_dong_id별 최신(PK 최대) 행만 쓰므로, 과거에 잘못 들어간 행이
 * 있어도 최신 값이 우선한다.
 */
@RestController
@RequestMapping("/api/admin/metrics")
@RequiredArgsConstructor
public class MetricAdminController {

    private final CultureMetricScoreService cultureMetricScoreService;
    private final TransitMetricScoreService transitMetricScoreService;
    private final SportsMetricScoreService sportsMetricScoreService;
    private final NatureMetricScoreService natureMetricScoreService;
    private final LifeConvenienceMetricScoreService lifeConvenienceMetricScoreService;
    private final HealthcareMetricScoreService healthcareMetricScoreService;
    private final FoodMetricScoreService foodMetricScoreService;
    private final SafetyMetricScoreService safetyMetricScoreService;

    @PostMapping("/recalculate-all")
    public ResponseEntity<ApiResponse<Void>> recalculateAll() {
        cultureMetricScoreService.recalculateAll();
        transitMetricScoreService.recalculateAll();
        sportsMetricScoreService.recalculateAll();
        natureMetricScoreService.recalculateAll();
        lifeConvenienceMetricScoreService.recalculateAll();
        healthcareMetricScoreService.recalculateAll();
        foodMetricScoreService.recalculateAll();
        safetyMetricScoreService.recalculateAll();

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
