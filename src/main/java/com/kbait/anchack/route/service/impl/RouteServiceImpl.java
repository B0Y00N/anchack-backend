package com.kbait.anchack.route.service.impl;

import com.kbait.anchack.route.client.KakaoGeocodingClient;
import com.kbait.anchack.route.client.KakaoTransitDirectionsClient;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.service.RouteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

/**
 * commuteType이 "대중교통"이면 카카오 대중교통 길찾기로 실제 계산한다.
 * "자가용"은 아직 미구현(공급자/방식 미정) - destAddress가 없는 조건에서는 호출되지
 * 않으므로(recommendation.service.CommuteFilter 참고) 자가용 조건이 아니면 앱 부팅에는
 * 영향 없다.
 */
@Service
@RequiredArgsConstructor
public class RouteServiceImpl implements RouteService {

    private static final String PUBLIC_TRANSIT = "대중교통";
    private static final String CAR = "자가용";

    private final KakaoGeocodingClient kakaoGeocodingClient;
    private final KakaoTransitDirectionsClient kakaoTransitDirectionsClient;

    @Override
    public Coordinates geocode(String address) {
        return kakaoGeocodingClient.geocode(address);
    }

    @Override
    public CommuteResult calculateCommute(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            BigDecimal destLatitude,
            BigDecimal destLongitude,
            String commuteType
    ) {
        if (PUBLIC_TRANSIT.equals(commuteType)) {
            return kakaoTransitDirectionsClient.findRoute(originLatitude, originLongitude, destLatitude, destLongitude);
        }

        if (CAR.equals(commuteType)) {
            throw new UnsupportedOperationException("자가용 통근 계산은 아직 구현되지 않았습니다.");
        }

        throw new IllegalArgumentException("지원하지 않는 통근 수단입니다: " + commuteType);
    }
}
