package com.kbait.anchack.recommendation.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** 통근 하드필터에서 경로 API 호출 출발지로 쓰는 행정동 좌표. */
@Getter
@Setter
@NoArgsConstructor
public class AdminDongLocation {

    private Long adminDongId;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
