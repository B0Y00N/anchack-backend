package com.kbait.anchack.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** admin_dong_id가 보유한 places.category 하나. 필수 인프라 하드필터에서 존재 여부 판단용. */
@Getter
@Setter
@NoArgsConstructor
public class AdminDongCategoryRow {

    private Long adminDongId;
    private String category;
}
