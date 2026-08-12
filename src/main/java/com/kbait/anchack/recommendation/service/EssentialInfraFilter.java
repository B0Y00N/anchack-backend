package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongCategoryRow;
import com.kbait.anchack.recommendation.mapper.EssentialInfraMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 필수 인프라 하드필터. condition_essentials 값(한글)을 places.category로 변환한 뒤,
 * 요구 카테고리 전부를 보유한 행정동만 통과시킨다(부분집합이 아니면 탈락).
 *
 * 다른 구현체가 나올 여지가 없는 단일 필터 로직이라 인터페이스 분리 없이
 * 구현체만 둠.
 */
@Component
@RequiredArgsConstructor
public class EssentialInfraFilter {

    private static final Map<String, String> ESSENTIAL_TO_PLACE_CATEGORY = Map.of(
            "편의점", "CONVENIENCE_STORE",
            "헬스장", "GYM",
            "병원", "HOSPITAL",
            "공원", "PARK",
            "대형마트", "MART"
    );

    private final EssentialInfraMapper essentialInfraMapper;

    public List<Long> filter(List<Long> candidateAdminDongIds, List<String> essentialCategories) {
        if (essentialCategories == null || essentialCategories.isEmpty()) {
            return candidateAdminDongIds;
        }

        List<String> requiredPlaceCategories = toPlaceCategories(essentialCategories);
        Map<Long, Set<String>> presentCategoriesByDong =
                findPresentCategoriesByDong(candidateAdminDongIds, requiredPlaceCategories);

        return candidateAdminDongIds.stream()
                .filter(adminDongId -> presentCategoriesByDong
                        .getOrDefault(adminDongId, Set.of())
                        .containsAll(requiredPlaceCategories))
                .toList();
    }

    private List<String> toPlaceCategories(List<String> essentialCategories) {
        return essentialCategories.stream()
                .map(ESSENTIAL_TO_PLACE_CATEGORY::get)
                .toList();
    }

    private Map<Long, Set<String>> findPresentCategoriesByDong(
            List<Long> adminDongIds,
            List<String> placeCategories
    ) {
        List<AdminDongCategoryRow> rows =
                essentialInfraMapper.findCategoriesByDongsAndCategories(adminDongIds, placeCategories);

        return rows.stream()
                .collect(Collectors.groupingBy(
                        AdminDongCategoryRow::getAdminDongId,
                        Collectors.mapping(AdminDongCategoryRow::getCategory, Collectors.toSet())
                ));
    }
}
