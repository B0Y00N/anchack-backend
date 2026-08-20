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
}
