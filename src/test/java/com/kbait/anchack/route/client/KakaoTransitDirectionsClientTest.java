package com.kbait.anchack.route.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.route.config.RouteApiProperties;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRouteResponse;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import com.kbait.anchack.route.exception.RouteNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
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

    @Test
    void 레이트리밋_429는_한번_재시도한_뒤_성공하면_정상_결과를_반환한다() throws Exception {
        KakaoTransitRouteResponse response = OBJECT_MAPPER.readValue(
                """
                {"status": "OK", "routes": [{"properties": {"totalTime": 1800, "transfers": 1}}]}
                """, KakaoTransitRouteResponse.class);
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenThrow(tooManyRequests())
                .thenReturn(ResponseEntity.ok(response));

        CommuteResult result = client.findRoute(COORD, COORD, COORD, COORD);

        assertThat(result.getCommuteTime()).isEqualTo(30);
        assertThat(result.getTransferCount()).isEqualTo(1);
    }

    @Test
    void 레이트리밋_429가_재시도_후에도_반복되면_예외를_던진다() {
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenThrow(tooManyRequests());

        assertThatThrownBy(() -> client.findRoute(COORD, COORD, COORD, COORD))
                .isInstanceOf(KakaoRouteApiException.class);
    }

    private HttpClientErrorException tooManyRequests() {
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    }
}
