package com.kbait.anchack.condition.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserConditionCreateRequest {

    private String destAddress;

    @Size(max = 2)
    private List<String> guCodes;

    @NotBlank
    private String commuteType;

    @PositiveOrZero
    private Integer maxCommuteTime;

    @PositiveOrZero
    private Integer maxTransferCount;

    @NotEmpty
    @Size(min = 1, max = 3)
    private List<String> priorityCategories;

    private List<String> essentialCategories;

    @NotBlank
    private String rentalType;

    @PositiveOrZero
    private Long maxDeposit;

    @PositiveOrZero
    private Integer maxRent;

    private List<String> preferredHouseTypes;

    @PositiveOrZero
    private BigDecimal minArea;
}
