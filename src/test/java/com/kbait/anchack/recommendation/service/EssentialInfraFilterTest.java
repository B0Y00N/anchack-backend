package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.AdminDongCategoryRow;
import com.kbait.anchack.recommendation.mapper.EssentialInfraMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EssentialInfraFilterTest {

    @Mock
    private EssentialInfraMapper essentialInfraMapper;

    private EssentialInfraFilter filter;

    @BeforeEach
    void setUp() {
        filter = new EssentialInfraFilter(essentialInfraMapper);
    }

    @Test
    void 필수_카테고리가_없으면_후보를_그대로_반환한다() {
        List<Long> candidates = List.of(1L, 2L);

        List<Long> result = filter.filter(candidates, List.of());

        assertThat(result).containsExactlyElementsOf(candidates);
        verifyNoInteractions(essentialInfraMapper);
    }

    @Test
    void 요구_카테고리를_모두_보유한_행정동만_통과한다() {
        List<Long> candidates = List.of(1L, 2L);
        when(essentialInfraMapper.findCategoriesByDongsAndCategories(candidates, List.of("HOSPITAL", "PARK")))
                .thenReturn(List.of(
                        categoryRow(1L, "HOSPITAL"),
                        categoryRow(1L, "PARK"),
                        categoryRow(2L, "HOSPITAL")
                ));

        List<Long> result = filter.filter(candidates, List.of("병원", "공원"));

        assertThat(result).containsExactly(1L);
    }

    @Test
    void 해당_카테고리가_전혀_없는_행정동은_탈락한다() {
        List<Long> candidates = List.of(1L);
        when(essentialInfraMapper.findCategoriesByDongsAndCategories(candidates, List.of("GYM")))
                .thenReturn(List.of());

        List<Long> result = filter.filter(candidates, List.of("헬스장"));

        assertThat(result).isEmpty();
    }

    private AdminDongCategoryRow categoryRow(Long adminDongId, String category) {
        AdminDongCategoryRow row = new AdminDongCategoryRow();
        row.setAdminDongId(adminDongId);
        row.setCategory(category);

        return row;
    }
}
