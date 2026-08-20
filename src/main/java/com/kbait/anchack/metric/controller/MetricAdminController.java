package com.kbait.anchack.metric.controller;

import com.kbait.anchack.common.response.ApiResponse;
import com.kbait.anchack.metric.service.CultureMetricScoreService;
import com.kbait.anchack.metric.service.FoodMetricScoreService;
import com.kbait.anchack.metric.service.HealthcareMetricScoreService;
import com.kbait.anchack.metric.service.LifeConvenienceMetricScoreService;
import com.kbait.anchack.metric.service.NatureMetricScoreService;
import com.kbait.anchack.metric.service.PropertyMetricAggregationService;
import com.kbait.anchack.metric.service.SafetyMetricScoreService;
import com.kbait.anchack.metric.service.SportsMetricScoreService;
import com.kbait.anchack.metric.service.TransitMetricScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지표 재계산을 수동으로 트리거하는 개발용 관리 엔드포인트.
 * 운영에서는 스케줄러가 places 기반 서비스를 개별 호출할 예정이라, 이 컨트롤러는
 * 로컬 데이터 채우기/재계산 용도로만 쓴다(silence는 places 기반이 아니라 여기 포함 안 함).
 *
 * places 기반 지표는 기존처럼 시계열로 누적한다. property_metrics는 별도 엔드포인트에서
 * 기존 내용을 삭제하고 현재 전체 임대차 거래를 집계해 단일 스냅샷으로 다시 만든다.
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
    private final PropertyMetricAggregationService propertyMetricAggregationService;

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

    @PostMapping("/property/recalculate")
    public ResponseEntity<ApiResponse<Void>> recalculatePropertyMetrics() {
        propertyMetricAggregationService.recalculate();

        return ResponseEntity.ok(ApiResponse.success(null));
    }
}
