package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.PropertyMetricCandidateRow;
import com.kbait.anchack.recommendation.mapper.PropertyMetricMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 주거유형 + 예산(보증금/월세/평수) 하드필터. preferred_house_types가 비어있으면
 * 전체 주거유형을 대상으로 하고, 선택한 유형 중 하나라도 예산 조건을 만족하면
 * 통과시킨다(ANY). 해당 admin_dong에 조회 대상 house_type의 property_metrics가
 * 아예 없으면 자동 탈락한다.
 *
 * 관리비(max_maintenance_fee)는 property_metrics에 대응 컬럼이 없어 필터에서 제외.
 *
 * 다른 구현체가 나올 여지가 없는 단일 필터 로직이라 인터페이스 분리 없이
 * 구현체만 둠.
 */
@Component
@RequiredArgsConstructor
public class HouseTypeBudgetFilter {

    private static final List<String> ALL_HOUSE_TYPES =
            List.of("오피스텔", "빌라", "단독", "다가구", "아파트", "원룸");

    private final PropertyMetricMapper propertyMetricMapper;

    public List<Long> filter(
            List<Long> candidateAdminDongIds,
            String rentalType,
            List<String> preferredHouseTypes,
            Long maxDeposit,
            Integer maxRent,
            BigDecimal minArea
    ) {
        List<String> houseTypes = (preferredHouseTypes == null || preferredHouseTypes.isEmpty())
                ? ALL_HOUSE_TYPES
                : preferredHouseTypes;

        List<PropertyMetricCandidateRow> rows = propertyMetricMapper.findLatestByDongsRentalTypeAndHouseTypes(
                candidateAdminDongIds, rentalType, houseTypes);

        Set<Long> passingAdminDongIds = rows.stream()
                .filter(row -> satisfiesBudget(row, maxDeposit, maxRent, minArea))
                .map(PropertyMetricCandidateRow::getAdminDongId)
                .collect(Collectors.toSet());

        return candidateAdminDongIds.stream()
                .filter(passingAdminDongIds::contains)
                .toList();
    }

    private boolean satisfiesBudget(
            PropertyMetricCandidateRow row,
            Long maxDeposit,
            Integer maxRent,
            BigDecimal minArea
    ) {
        boolean depositOk = maxDeposit == null || row.getAvgDeposit() <= maxDeposit;
        boolean rentOk = maxRent == null || row.getAvgRent() <= maxRent;
        boolean areaOk = minArea == null || row.getAvgArea().compareTo(minArea) >= 0;

        return depositOk && rentOk && areaOk;
    }
}
