package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.dto.DensityScoreInput;
import com.kbait.anchack.metric.dto.LifeConvenienceMetricRow;
import com.kbait.anchack.metric.mapper.LifeConvenienceMetricMapper;
import com.kbait.anchack.metric.service.DensityMinMaxScoreCalculator;
import com.kbait.anchack.metric.service.LifeConvenienceMetricScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * convenience_score 재계산 진입점 (카테고리 여러 개짜리 지표 템플릿).
 * mart/bank/department_store 각각을 독립적으로 정규화한 뒤 평균낸다 — DensityMinMaxScoreCalculator 참고.
 */
@Service
@RequiredArgsConstructor
public class LifeConvenienceMetricScoreServiceImpl implements LifeConvenienceMetricScoreService {

    private final LifeConvenienceMetricMapper lifeConvenienceMetricMapper;
    private final DensityMinMaxScoreCalculator calculator;

    @Override
    @Transactional
    public void recalculateAll() {
        List<LifeConvenienceMetricRow> rows = lifeConvenienceMetricMapper.findAllCountsFromPlaces();
        List<DensityScoreInput> inputs = rows.stream().map(this::toInput).toList();

        Map<Long, BigDecimal> scores = calculator.calculate(inputs);

        rows.forEach(row -> row.setScore(scores.get(row.getAdminDongId())));

        lifeConvenienceMetricMapper.insertRows(rows);
    }

    private DensityScoreInput toInput(LifeConvenienceMetricRow row) {
        return DensityScoreInput.builder()
                .adminDongId(row.getAdminDongId())
                .counts(Map.of(
                        "mart", row.getMartCount(),
                        "bank", row.getBankCount(),
                        "departmentStore", row.getDepartmentStoreCount()
                ))
                .dongPopulation(row.getDongPopulation())
                .dongArea(row.getDongArea())
                .build();
    }
}
