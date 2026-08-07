package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/**
 * 하드필터 파이프라인 입력값. user_conditions와 하위 테이블(condition_gus,
 * condition_essentials, preferred_house_types)을 조합한 값으로, condition 도메인이
 * 아직 없어 우선 recommendation.dto에 둠 - condition 도메인 구축 시 그쪽으로 옮기거나
 * 그쪽 결과를 이 타입으로 변환하는 어댑터를 두는 걸 검토.
 */
@Getter
@Builder
public class ConditionBundle {

    private Long conditionId;
    private String rentalType;
    private List<String> guCodes;
    private List<String> essentialCategories;
    private List<String> preferredHouseTypes;
    private Long maxDeposit;
    private Integer maxRent;
    private BigDecimal minArea;
    private String destAddress;
    private String commuteType;
    private Integer maxCommuteTime;
    private Integer maxTransferCount;
}
