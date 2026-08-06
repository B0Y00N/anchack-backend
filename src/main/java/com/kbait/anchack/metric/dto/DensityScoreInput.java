package com.kbait.anchack.metric.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.Map;

/**
 * 소음·치안을 제외한 정적 지표(문화, 교통, 의료, 음식, 생활편의, 체육, 자연) 공통 계산 입력.
 *
 * counts는 카운트 종류별 이름 -> 원본 개수. 원본 카운트가 1개뿐인 테이블(culture_count 등)은
 * 항목을 1개만 넣으면 되고, 여러 개인 테이블(mart/bank/department_store 등)은 컬럼별로 각각 넣는다.
 * DensityMinMaxScoreCalculator가 각 항목을 독립적으로 정규화한 뒤 평균낸다.
 *
 * MyBatis가 아니라 서비스 코드에서 직접 생성하는 객체라 @Builder만 사용한다.
 */
@Getter
@Builder
public class DensityScoreInput {

    private Long adminDongId;
    private Map<String, Long> counts;
    private BigDecimal dongPopulation;
    private BigDecimal dongArea;
}
