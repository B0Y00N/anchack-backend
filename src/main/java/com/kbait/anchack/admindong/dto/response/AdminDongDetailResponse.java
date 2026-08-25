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
    private BigDecimal dongPopulation;
    private BigDecimal lat;
    private BigDecimal lng;

    private Long deposit;
    private Long monthly;
    private List<RentDistBucket> rentDist;
    private List<HouseTypeMetricResponse> monthlyHouseTypes;
    private Long jeonseDeposit;
    private List<RentDistBucket> jeonseDist;
    private List<HouseTypeMetricResponse> jeonseHouseTypes;

    private Integer cctv;
    private Integer safetyBellCount;
    private BigDecimal safetyScoreMax;
    private String police;
    private BigDecimal crimeRate;
    private BigDecimal safetyScore;

    private Integer gyms;
    private Integer convenience;
    private Integer hospitals;
    private Integer pharmacies;
    private Integer banks;
    private Integer cafes;
    private Integer restaurants;
    private Integer parks;
    private Integer department;
    private Integer mart;

    private Integer subwayStationCount;
    private Integer busStopCount;
    private BigDecimal transitScore;
    private String nearestSubwayStation;
    private String nearestBusStop;
}
