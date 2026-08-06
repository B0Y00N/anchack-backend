package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.dto.CultureMetricRow;
import com.kbait.anchack.metric.dto.DensityScoreInput;
import com.kbait.anchack.metric.mapper.CultureMetricMapper;
import com.kbait.anchack.metric.service.CultureMetricScoreService;
import com.kbait.anchack.metric.service.DensityMinMaxScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * culture_metrics 재계산 진입점 (카테고리 1개짜리 지표 템플릿).
 * places에서 CULTURE 카테고리 개수를 집계하고, 점수를 계산해서 새 행으로 저장한다.
 * places는 스케줄러가 지속적으로 갱신하므로, 이 서비스는 스케줄러가 트리거할 때마다
 * 그 시점 기준 스냅샷 한 행을 새로 쌓는다(시계열).
 */
@Service
@RequiredArgsConstructor
public class CultureMetricScoreServiceImpl implements CultureMetricScoreService {

    private final CultureMetricMapper cultureMetricMapper;
    private final DensityMinMaxScoreCalculator calculator;

    @Override
    @Transactional
    public void recalculateAll() {
        List<CultureMetricRow> rows = cultureMetricMapper.findAllCountsFromPlaces();
        List<DensityScoreInput> inputs = rows.stream().map(this::toInput).toList();

        Map<Long, BigDecimal> scores = calculator.calculate(inputs);

        rows.forEach(row -> row.setScore(scores.get(row.getAdminDongId())));

        cultureMetricMapper.insertRows(rows);
    }

    private DensityScoreInput toInput(CultureMetricRow row) {
        return DensityScoreInput.builder()
                .adminDongId(row.getAdminDongId())
                .counts(Map.of("culture", row.getCultureCount()))
                .dongPopulation(row.getDongPopulation())
                .dongArea(row.getDongArea())
                .build();
    }
}
