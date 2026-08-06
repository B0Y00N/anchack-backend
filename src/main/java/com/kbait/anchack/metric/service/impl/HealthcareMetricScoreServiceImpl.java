package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.dto.DensityScoreInput;
import com.kbait.anchack.metric.dto.HealthcareMetricRow;
import com.kbait.anchack.metric.mapper.HealthcareMetricMapper;
import com.kbait.anchack.metric.service.DensityMinMaxScoreCalculator;
import com.kbait.anchack.metric.service.HealthcareMetricScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class HealthcareMetricScoreServiceImpl implements HealthcareMetricScoreService {

    private final HealthcareMetricMapper healthcareMetricMapper;
    private final DensityMinMaxScoreCalculator calculator;

    @Override
    @Transactional
    public void recalculateAll() {
        List<HealthcareMetricRow> rows = healthcareMetricMapper.findAllCountsFromPlaces();
        List<DensityScoreInput> inputs = rows.stream().map(this::toInput).toList();

        Map<Long, BigDecimal> scores = calculator.calculate(inputs);

        rows.forEach(row -> row.setScore(scores.get(row.getAdminDongId())));

        healthcareMetricMapper.insertRows(rows);
    }

    private DensityScoreInput toInput(HealthcareMetricRow row) {
        return DensityScoreInput.builder()
                .adminDongId(row.getAdminDongId())
                .counts(Map.of(
                        "hospital", row.getHospitalCount(),
                        "pharmacy", row.getPharmacyCount()
                ))
                .dongPopulation(row.getDongPopulation())
                .dongArea(row.getDongArea())
                .build();
    }
}
