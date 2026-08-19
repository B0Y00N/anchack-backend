package com.kbait.anchack.route.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommuteResult {

    private Integer commuteTime;
    private Integer transferCount;

    /**
     * 아래 5개 필드는 routes[0]의 첫 번째 non-walking(SUBWAY/BUS) 스텝 기준이다.
     * 환승이 있어 non-walking 스텝이 여러 개면 route만 전체 구간을 이어붙이고,
     * transportType/lineNum/vehicleType은 맨 처음 타는 노선 하나만 담는다.
     */
    private String route;
    private String transportType;
    private String lineNum;
    private String vehicleType;
    private Integer walkMin;
    private Integer subwayMin;
}
