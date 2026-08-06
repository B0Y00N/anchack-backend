package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.dto.SafetyScoreInput;
import com.kbait.anchack.metric.mapper.SafetyMetricMapper;
import com.kbait.anchack.metric.service.SafetyMetricScoreService;
import com.kbait.anchack.metric.service.SafetyScoreCalculator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SafetyMetricScoreServiceImpl implements SafetyMetricScoreService {

    private final SafetyMetricMapper safetyMetricMapper;
    private final SafetyScoreCalculator calculator;

    @Override
    @Transactional
    public void recalculateAll() {
        List<SafetyScoreInput> rows = safetyMetricMapper.findAllCountsFromPlaces();
        Map<Long, BigDecimal> scores = calculator.calculate(rows);

        rows.forEach(row -> row.setScore(scores.get(row.getAdminDongId())));

        safetyMetricMapper.insertRows(rows);
    }
}
