package com.kbait.anchack.route.service.impl;

import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.service.RouteService;

import java.math.BigDecimal;

/**
 * 카카오 API를 호출하지 않고 고정값을 즉시 반환하는 RouteService 스텁. 카카오 대중교통
 * 길찾기는 애플리케이션 전체에 12.5 req/s 페이싱이 걸려 있어(KakaoTransitDirectionsClient
 * 참고) destAddress가 있는 부하테스트 시나리오에서는 이 페이싱이 병목을 가려버린다.
 * route.mode 프로퍼티가 "stub"일 때 RouteConfig가 이 구현을 등록해 나머지 추천 파이프라인
 * (하드필터 이후 스코어링/DB 부하 등)만 순수하게 측정할 수 있게 한다.
 */
public class StubRouteService implements RouteService {

    private static final BigDecimal STUB_LATITUDE = new BigDecimal("37.5665");
    private static final BigDecimal STUB_LONGITUDE = new BigDecimal("126.9780");
    private static final int STUB_COMMUTE_TIME_MIN = 30;
    private static final int STUB_TRANSFER_COUNT = 1;
    private static final int STUB_WALK_MIN = 8;
    private static final int STUB_TRANSIT_MIN = 22;

    @Override
    public Coordinates geocode(String address) {
        return Coordinates.builder()
                .latitude(STUB_LATITUDE)
                .longitude(STUB_LONGITUDE)
                .build();
    }

    @Override
    public CommuteResult calculateCommute(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            BigDecimal destLatitude,
            BigDecimal destLongitude,
            String commuteType
    ) {
        return CommuteResult.builder()
                .commuteTime(STUB_COMMUTE_TIME_MIN)
                .transferCount(STUB_TRANSFER_COUNT)
                .route("스텁 경로 (부하테스트용)")
                .transportType("SUBWAY")
                .lineNum("2호선")
                .vehicleType("SUBWAY")
                .walkMin(STUB_WALK_MIN)
                .transitMin(STUB_TRANSIT_MIN)
                .build();
    }
}
