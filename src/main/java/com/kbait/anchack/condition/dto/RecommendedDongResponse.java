package com.kbait.anchack.condition.dto;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@Builder
public class RecommendedDongResponse {

    private Long adminDongId;
    private String guName;
    private String dongName;
    private BigDecimal lat;
    private BigDecimal lng;
    private BigDecimal totalScore;
    private BigDecimal dataCoverageRate;
    private Integer rank;
    private Integer commuteTime;
    private Integer transferCount;

    /**
     * transportType/lineNum/vehicleType/route는 첫 번째로 타는 노선 기준(환승 시 이후 구간은
     * route에만 이어붙어 남는다), walkMin/transitMin은 도보/대중교통(버스+지하철) 탑승 각각의
     * 총합이다. commuteTime에서 walkMin+transitMin을 뺀 나머지는 대기/환승 시간이며 별도
     * 필드로 내려주지 않는다 - 프론트에서 이 값으로 역산할 경우 위 정의를 그대로 따라야
     * 음수가 나오지 않는다.
     */
    private String route;
    private String transportType;
    private String lineNum;
    private String vehicleType;
    private Integer walkMin;
    private Integer transitMin;
    private String recommendationReason;
    private String caution;
}
