package com.kbait.anchack.route.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class Coordinates {

    private BigDecimal latitude;
    private BigDecimal longitude;
}
