package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Builder
public class SavedConditionResponse {

    private Long conditionId;
    private String title;
    private String rentalType;
    private String destAddress;
    private String commuteType;
    private Integer maxCommuteTime;
    private Integer maxTransferCount;
    private BigDecimal minArea;
    private Long maxDeposit;
    private Integer maxRent;
    private LocalDateTime createdAt;

    /** 조건 생성 이후 관련 데이터(admin_dong 지표 등)가 갱신돼 결과가 최신이 아닐 수
     * 있는지 여부. */
    private Boolean latest;
}
