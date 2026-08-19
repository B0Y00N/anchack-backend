package com.kbait.anchack.admindong.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

/** API_USER_CONDITIONS_REVISION_REQUEST.md P1-b(행정동 고정 정보) 응답 1건. */
@Getter
@Builder
public class AdminDongDetailResponse {

    private Long adminDongId;
    private String guName;
    private String dongName;
    private BigDecimal lat;
    private BigDecimal lng;

    private Long deposit;
    private Long monthly;
    private List<RentDistBucket> rentDist;

    private BigDecimal cctv;
    private String police;
    private BigDecimal crimeRate;
    private BigDecimal safetyScore;

    private Integer gyms;
    private Integer convenience;
    private Integer hospitals;
    private Integer parks;
    private Integer department;
    private Integer mart;
}
