package com.kbait.anchack.metric.service;

import com.kbait.anchack.metric.mapper.PropertyMetricAggregationMapper;
import com.kbait.anchack.metric.service.impl.PropertyMetricAggregationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PropertyMetricAggregationServiceTest {

    @Mock
    private PropertyMetricAggregationMapper propertyMetricAggregationMapper;

    private PropertyMetricAggregationService propertyMetricAggregationService;

    @BeforeEach
    void setUp() {
        propertyMetricAggregationService =
                new PropertyMetricAggregationServiceImpl(propertyMetricAggregationMapper);
    }

    @Test
    void DELETE_후_INSERT를_각각_한_번_순서대로_호출한다() {
        propertyMetricAggregationService.recalculate();

        InOrder inOrder = inOrder(propertyMetricAggregationMapper);
        inOrder.verify(propertyMetricAggregationMapper).deleteAll();
        inOrder.verify(propertyMetricAggregationMapper).insertAggregatedFromRentalTransactions();
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void INSERT_영향_행_수를_그대로_반환한다() {
        int expectedInsertedRows = 37;
        when(propertyMetricAggregationMapper.insertAggregatedFromRentalTransactions())
                .thenReturn(expectedInsertedRows);

        int actual = propertyMetricAggregationService.recalculate();

        assertThat(actual).isEqualTo(expectedInsertedRows);
    }

    @Test
    void INSERT_예외를_동일한_객체로_전파한다() {
        RuntimeException insertException = new RuntimeException("INSERT_FAILURE");
        when(propertyMetricAggregationMapper.insertAggregatedFromRentalTransactions())
                .thenThrow(insertException);

        Throwable actual = catchThrowable(propertyMetricAggregationService::recalculate);

        assertThat(actual).isSameAs(insertException);
        InOrder inOrder = inOrder(propertyMetricAggregationMapper);
        inOrder.verify(propertyMetricAggregationMapper).deleteAll();
        inOrder.verify(propertyMetricAggregationMapper).insertAggregatedFromRentalTransactions();
        inOrder.verifyNoMoreInteractions();
    }
}
