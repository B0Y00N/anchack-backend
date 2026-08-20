package com.kbait.anchack.metric.service.impl;

import com.kbait.anchack.metric.mapper.PropertyMetricAggregationMapper;
import com.kbait.anchack.metric.service.PropertyMetricAggregationService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PropertyMetricAggregationServiceImpl implements PropertyMetricAggregationService {

    private static final Logger log = LoggerFactory.getLogger(PropertyMetricAggregationServiceImpl.class);

    private final PropertyMetricAggregationMapper propertyMetricAggregationMapper;

    @Override
    @Transactional
    public int recalculate() {
        propertyMetricAggregationMapper.deleteAll();
        int insertedRows = propertyMetricAggregationMapper.insertAggregatedFromRentalTransactions();

        log.info("property_metrics snapshot recalculation completed: insertedRows={}", insertedRows);

        return insertedRows;
    }
}
