package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.dto.DensityScoreInput;
import com.kbait.anchack.metric.dto.SportsMetricRow;
import com.kbait.anchack.metric.mapper.SportsMetricMapper;
import com.kbait.anchack.metric.service.DensityMinMaxScoreCalculator;
import com.kbait.anchack.metric.service.SportsMetricScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SportsMetricScoreServiceImpl implements SportsMetricScoreService {

    private final SportsMetricMapper sportsMetricMapper;
    private final DensityMinMaxScoreCalculator calculator;

    @Override
    @Transactional
    public void recalculateAll() {
        List<SportsMetricRow> rows = sportsMetricMapper.findAllCountsFromPlaces();
        List<DensityScoreInput> inputs = rows.stream().map(this::toInput).toList();

        Map<Long, BigDecimal> scores = calculator.calculate(inputs);

        rows.forEach(row -> row.setScore(scores.get(row.getAdminDongId())));

        sportsMetricMapper.insertRows(rows);
    }

    private DensityScoreInput toInput(SportsMetricRow row) {
        return DensityScoreInput.builder()
                .adminDongId(row.getAdminDongId())
                .counts(Map.of("sports", row.getSportsCount()))
                .dongPopulation(row.getDongPopulation())
                .dongArea(row.getDongArea())
                .build();
    }
}
