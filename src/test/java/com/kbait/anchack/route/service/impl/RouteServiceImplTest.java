package com.kbait.anchack.route.service.impl;

import com.kbait.anchack.route.client.KakaoGeocodingClient;
import com.kbait.anchack.route.client.KakaoTransitDirectionsClient;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.Coordinates;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RouteServiceImplTest {

    private static final BigDecimal ORIGIN_LAT = new BigDecimal("37.5");
    private static final BigDecimal ORIGIN_LNG = new BigDecimal("127.0");
    private static final BigDecimal DEST_LAT = new BigDecimal("37.6");
    private static final BigDecimal DEST_LNG = new BigDecimal("127.1");

    @Mock
    private KakaoGeocodingClient kakaoGeocodingClient;

    @Mock
    private KakaoTransitDirectionsClient kakaoTransitDirectionsClient;

    private RouteServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new RouteServiceImpl(kakaoGeocodingClient, kakaoTransitDirectionsClient);
    }

    @Test
    void geocode는_KakaoGeocodingClient에_위임한다() {
        Coordinates coordinates = Coordinates.builder().latitude(DEST_LAT).longitude(DEST_LNG).build();
        when(kakaoGeocodingClient.geocode("서울시청")).thenReturn(coordinates);

        Coordinates result = service.geocode("서울시청");

        assertThat(result).isEqualTo(coordinates);
    }

    @Test
    void 대중교통은_KakaoTransitDirectionsClient를_호출한다() {
        CommuteResult commuteResult = CommuteResult.builder().commuteTime(30).transferCount(1).build();
        when(kakaoTransitDirectionsClient.findRoute(ORIGIN_LAT, ORIGIN_LNG, DEST_LAT, DEST_LNG))
                .thenReturn(commuteResult);

        CommuteResult result = service.calculateCommute(ORIGIN_LAT, ORIGIN_LNG, DEST_LAT, DEST_LNG, "대중교통");

        assertThat(result).isEqualTo(commuteResult);
        verify(kakaoTransitDirectionsClient).findRoute(ORIGIN_LAT, ORIGIN_LNG, DEST_LAT, DEST_LNG);
    }

    @Test
    void 자가용은_아직_미구현이라_예외가_발생한다() {
        assertThatThrownBy(() -> service.calculateCommute(ORIGIN_LAT, ORIGIN_LNG, DEST_LAT, DEST_LNG, "자가용"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void 지원하지_않는_통근수단은_IllegalArgumentException이_발생한다() {
        assertThatThrownBy(() -> service.calculateCommute(ORIGIN_LAT, ORIGIN_LNG, DEST_LAT, DEST_LNG, "자전거"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
