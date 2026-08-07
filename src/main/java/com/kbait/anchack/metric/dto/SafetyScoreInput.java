package com.kbait.anchack.metric.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * MyBatis가 SafetyMetricMapper의 조회 결과를 이 클래스로 직접 매핑하므로
 * @NoArgsConstructor + @Setter가 필요하다 (프레임워크가 리플렉션으로 생성).
 * 테스트 등에서 수동으로 만들 때는 @Builder를 쓴다
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SafetyScoreInput {

    private Long adminDongId;
    private BigDecimal dongPopulation;
    private Integer cctvCount;
    private Integer streetLightCount;
    private Integer policeOfficeCount;
    private Integer safetyBellCount;

    /** gu_crime_stats.crime_rate — 인구 1만명당 범죄 발생 건수, 같은 구의 행정동은 동일한 값 */
    private BigDecimal crimeRatePer10k;

    /** 계산기 실행 후 서비스가 채워 넣는 최종 점수 (조회 시점엔 비어있음) */
    private BigDecimal score;
}
