package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.dto.DensityScoreInput;
import com.kbait.anchack.metric.dto.TransitMetricRow;
import com.kbait.anchack.metric.mapper.TransitMetricMapper;
import com.kbait.anchack.metric.service.DensityMinMaxScoreCalculator;
import com.kbait.anchack.metric.service.TransitMetricScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TransitMetricScoreServiceImpl implements TransitMetricScoreService {

    private final TransitMetricMapper transitMetricMapper;
    private final DensityMinMaxScoreCalculator calculator;

    @Override
    @Transactional
    public void recalculateAll() {
        List<TransitMetricRow> rows = transitMetricMapper.findAllCountsFromPlaces();
        List<DensityScoreInput> inputs = rows.stream().map(this::toInput).toList();

        Map<Long, BigDecimal> scores = calculator.calculate(inputs);

        rows.forEach(row -> row.setScore(scores.get(row.getAdminDongId())));

        transitMetricMapper.insertRows(rows);
    }

    private DensityScoreInput toInput(TransitMetricRow row) {
        return DensityScoreInput.builder()
                .adminDongId(row.getAdminDongId())
                .counts(Map.of(
                        "subwayStation", row.getSubwayStationCount(),
                        "busStop", row.getBusStopCount()
                ))
                .dongPopulation(row.getDongPopulation())
                .dongArea(row.getDongArea())
                .build();
    }
}
