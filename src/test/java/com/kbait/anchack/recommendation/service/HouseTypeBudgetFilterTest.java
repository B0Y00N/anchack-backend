package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.PropertyMetricCandidateRow;
import com.kbait.anchack.recommendation.mapper.PropertyMetricMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HouseTypeBudgetFilterTest {

    @Mock
    private PropertyMetricMapper propertyMetricMapper;

    private HouseTypeBudgetFilter filter;

    @BeforeEach
    void setUp() {
        filter = new HouseTypeBudgetFilter(propertyMetricMapper);
    }

    @Test
    void 선택한_주거유형_중_하나라도_예산을_만족하면_통과한다() {
        List<Long> candidates = List.of(1L);
        when(propertyMetricMapper.findLatestByDongsRentalTypeAndHouseTypes(any(), anyString(), anyList()))
                .thenReturn(List.of(
                        propertyMetricRow(1L, "아파트", 50_000_000L, 2_000_000L, "60.00"),
                        propertyMetricRow(1L, "원룸", 5_000_000L, 500_000L, "20.00")
                ));

        List<Long> result = filter.filter(
                candidates, "월세", List.of("아파트", "원룸"), 10_000_000L, 600_000, new BigDecimal("18.00"));

        assertThat(result).containsExactly(1L);
    }

    @Test
    void 모든_주거유형이_예산을_벗어나면_탈락한다() {
        List<Long> candidates = List.of(1L);
        when(propertyMetricMapper.findLatestByDongsRentalTypeAndHouseTypes(any(), anyString(), anyList()))
                .thenReturn(List.of(propertyMetricRow(1L, "아파트", 50_000_000L, 2_000_000L, "60.00")));

        List<Long> result = filter.filter(
                candidates, "월세", List.of("아파트"), 10_000_000L, 600_000, new BigDecimal("18.00"));

        assertThat(result).isEmpty();
    }

    @Test
    void 해당_조합의_property_metrics가_없는_행정동은_탈락한다() {
        List<Long> candidates = List.of(1L);
        when(propertyMetricMapper.findLatestByDongsRentalTypeAndHouseTypes(any(), anyString(), anyList()))
                .thenReturn(List.of());

        List<Long> result = filter.filter(
                candidates, "월세", List.of("아파트"), 10_000_000L, 600_000, new BigDecimal("18.00"));

        assertThat(result).isEmpty();
    }

    @Test
    void 예산_조건이_없으면_해당_항목은_검사하지_않는다() {
        List<Long> candidates = List.of(1L);
        when(propertyMetricMapper.findLatestByDongsRentalTypeAndHouseTypes(any(), anyString(), anyList()))
                .thenReturn(List.of(propertyMetricRow(1L, "아파트", 90_000_000L, 3_000_000L, "60.00")));

        List<Long> result = filter.filter(candidates, "월세", List.of("아파트"), null, null, null);

        assertThat(result).containsExactly(1L);
    }

    private PropertyMetricCandidateRow propertyMetricRow(
            Long adminDongId,
            String houseType,
            Long avgDeposit,
            Long avgRent,
            String avgArea
    ) {
        PropertyMetricCandidateRow row = new PropertyMetricCandidateRow();
        row.setAdminDongId(adminDongId);
        row.setHouseType(houseType);
        row.setAvgDeposit(avgDeposit);
        row.setAvgRent(avgRent);
        row.setAvgArea(new BigDecimal(avgArea));

        return row;
    }
}
