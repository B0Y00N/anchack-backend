package com.kbait.anchack.admindong.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class AdminDong {

    private Long adminDongId;
    private String guCode;
    private String dongCode;
    private String name;

    // JOIN 조회 결과
    private String guName;

    private BigDecimal dongPopulation;
    private BigDecimal dongArea;
    private BigDecimal latitude;
    private BigDecimal longitude;
}
