package com.kbait.anchack.route.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommuteResult {

    private Integer commuteTime;
    private Integer transferCount;

    /**
     * 아래 6개 필드는 기준이 서로 다르다.
     * - route: non-walking(SUBWAY/BUS) 스텝을 등장 순서대로 전부 이어붙인 전체 경로 요약.
     * - transportType: 전체 non-walking 스텝의 구성(BUS/SUBWAY/BUS_AND_SUBWAY)이다.
     * - lineNum/vehicleType: non-walking 스텝 중 "첫 번째"(맨 처음 타는 노선) 하나만
     *   대표로 담는다. 환승 이후 구간 정보는 route에만 남는다.
     * - walkMin/transitMin: 특정 스텝 하나가 아니라 WALKING 스텝 전체 합 / non-walking(BUS+SUBWAY)
     *   스텝 전체 합이다. transitMin은 지하철뿐 아니라 버스 탑승 시간도 포함한다 - 버스만으로
     *   구성된 통근도 값이 0이 되지 않아야 프론트 도넛차트의 탑승 구간이 항상 채워진다.
     *
     * commuteTime(총 소요시간)에서 walkMin+transitMin을 빼고 남는 시간은 대기/환승 시간이다.
     * 이 세 값의 기준(어떤 스텝을 포함/제외하는지)이 어긋나면 그 나머지가 음수가 될 수 있으므로,
     * 프론트가 역산하는 계약이라면 API 문서에도 이 관계를 명시해야 한다.
     */
    private String route;
    private String transportType;
    private String lineNum;
    private String vehicleType;
    private Integer walkMin;
    private Integer transitMin;
}
