package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.dto.DensityScoreInput;
import com.kbait.anchack.metric.dto.FoodMetricRow;
import com.kbait.anchack.metric.mapper.FoodMetricMapper;
import com.kbait.anchack.metric.service.DensityMinMaxScoreCalculator;
import com.kbait.anchack.metric.service.FoodMetricScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FoodMetricScoreServiceImpl implements FoodMetricScoreService {

    private final FoodMetricMapper foodMetricMapper;
    private final DensityMinMaxScoreCalculator calculator;

    @Override
    @Transactional
    public void recalculateAll() {
        List<FoodMetricRow> rows = foodMetricMapper.findAllCountsFromPlaces();
        List<DensityScoreInput> inputs = rows.stream().map(this::toInput).toList();

        Map<Long, BigDecimal> scores = calculator.calculate(inputs);

        rows.forEach(row -> row.setScore(scores.get(row.getAdminDongId())));

        foodMetricMapper.insertRows(rows);
    }

    private DensityScoreInput toInput(FoodMetricRow row) {
        return DensityScoreInput.builder()
                .adminDongId(row.getAdminDongId())
                .counts(Map.of(
                        "restaurant", row.getRestaurantCount(),
                        "cafe", row.getCafeCount()
                ))
                .dongPopulation(row.getDongPopulation())
                .dongArea(row.getDongArea())
                .build();
    }
}
