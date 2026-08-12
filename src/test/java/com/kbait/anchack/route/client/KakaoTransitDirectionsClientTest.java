package com.kbait.anchack.route.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.route.config.RouteApiProperties;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRouteResponse;
import com.kbait.anchack.route.exception.RouteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KakaoTransitDirectionsClientTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final BigDecimal COORD = new BigDecimal("37.5");

    @Mock
    private RestTemplate restTemplate;

    private KakaoTransitDirectionsClient client;

    @BeforeEach
    void setUp() {
        RouteApiProperties properties = new RouteApiProperties("test-key", 5000, 5000);
        client = new KakaoTransitDirectionsClient(restTemplate, properties);
    }

    @Test
    void status는_OK인데_routes가_비어있으면_경로없음_예외를_던진다() throws Exception {
        KakaoTransitRouteResponse response = OBJECT_MAPPER.readValue(
                """
                {"status": "OK", "routes": []}
                """, KakaoTransitRouteResponse.class);
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        assertThatThrownBy(() -> client.findRoute(COORD, COORD, COORD, COORD))
                .isInstanceOf(RouteNotFoundException.class);
    }

    @Test
    void routes는_있는데_properties가_null이면_경로없음_예외를_던진다() throws Exception {
        KakaoTransitRouteResponse response = OBJECT_MAPPER.readValue(
                """
                {"status": "OK", "routes": [{"properties": null}]}
                """, KakaoTransitRouteResponse.class);
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        assertThatThrownBy(() -> client.findRoute(COORD, COORD, COORD, COORD))
                .isInstanceOf(RouteNotFoundException.class);
    }
}
