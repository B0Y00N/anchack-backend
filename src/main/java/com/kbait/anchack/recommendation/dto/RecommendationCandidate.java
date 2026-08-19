package com.kbait.anchack.recommendation.dto;

import lombok.Builder;
import lombok.Getter;

/** 하드필터를 모두 통과한 행정동. commuteTime/transferCount는 destAddress가 없으면 null. */
@Getter
@Builder
public class RecommendationCandidate {

    private Long adminDongId;
    private Integer commuteTime;
    private Integer transferCount;
    private String route;
    private String transportType;
    private String lineNum;
    private String vehicleType;
    private Integer walkMin;
    private Integer subwayMin;
}
