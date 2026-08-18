package com.kbait.anchack.recommendation.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * recommendations INSERT 페이로드 겸 반환값. MyBatis가 useGeneratedKeys로 INSERT 후
 * recommendationId를 setter로 채워 넣어야 해서 @NoArgsConstructor + @Setter가 필요하다
 * (RecommendationScoreInput과 동일한 이유로 @Builder가 전체 인자 생성자를 쓰려면
 * @AllArgsConstructor를 명시해야 함).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommendationRow {

    private Long recommendationId;
    private Long conditionId;
    private Long adminDongId;
    private BigDecimal totalScore;
    private BigDecimal dataCoverageRate;
    private Integer commuteTime;
    private Integer transferCount;
    private Integer rank;
    private String recommendationReason;
    private String caution;

    /** INSERT 시점엔 쓰지 않고, recommendation_scores 행을 만들 때만 참조한다. */
    private List<CategoryScoreBreakdown> categoryBreakdowns;

    /**
     * admin_dongs/gus 표시용 정보. recommendations 테이블 컬럼이 아니라 응답 조립
     * 전용이라 insertBatch의 INSERT 문에는 참조되지 않는다(추가해도 INSERT에는 영향 없음).
     */
    private String guName;
    private String dongName;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
