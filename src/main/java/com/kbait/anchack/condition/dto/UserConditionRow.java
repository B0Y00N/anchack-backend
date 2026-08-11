package com.kbait.anchack.condition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * user_conditions INSERT 페이로드 겸 반환값. MyBatis가 useGeneratedKeys로 INSERT 후
 * conditionId를 setter로 채워 넣어야 해서 @NoArgsConstructor + @Setter가 필요하다
 * (RecommendationRow와 동일한 이유로 @Builder가 전체 인자 생성자를 쓰려면
 * @AllArgsConstructor를 명시해야 함).
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
}
