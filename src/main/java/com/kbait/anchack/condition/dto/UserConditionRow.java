package com.kbait.anchack.condition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * user_conditions INSERT 페이로드 겸 반환값. MyBatis가 useGeneratedKeys로 INSERT 후
 * conditionId를 setter로 채워 넣어야 해서 @NoArgsConstructor + @Setter가 필요하다
 * (RecommendationRow와 동일한 이유로 @Builder가 전체 인자 생성자를 쓰려면
 * @AllArgsConstructor를 명시해야 함).
 *
 * createdAt은 INSERT 시점엔 쓰지 않는다(컬럼 기본값 CURRENT_TIMESTAMP를 그대로 사용) -
 * findById/findSavedByUserId처럼 이미 저장된 조건을 조회할 때만 채워진다.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserConditionRow {

    private Long conditionId;
    private Long userId;
    private String title;
    private String rentalType;
    private String destAddress;
    private String commuteType;
    private Integer maxCommuteTime;
    private Integer maxTransferCount;
    private BigDecimal minArea;
    private Long maxDeposit;
    private Long maxRent;
    private LocalDateTime createdAt;

    /** is_latest 컬럼. INSERT 시점엔 쓰지 않고(컬럼 기본값 TRUE 그대로 사용) createdAt과
     * 동일하게 조회 전용이다. */
    private Boolean latest;
}
