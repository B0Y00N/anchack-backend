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

    @Test
    void 환승없이_지하철_한_구간이면_해당_스텝_기준으로_통근_상세를_채운다() throws Exception {
        KakaoTransitRouteResponse response = OBJECT_MAPPER.readValue(
                """
                {"status": "OK", "routes": [{
                  "properties": {"totalTime": 1095, "transfers": 0},
                  "steps": [
                    {"properties": {"type": "WALKING", "time": 60, "vehicles": [], "stops": [{"name": "출발지"}, {"name": "증산역"}]}},
                    {"properties": {"type": "SUBWAY", "time": 900, "vehicles": [{"name": "6호선", "type": "일반"}], "stops": [{"name": "증산(명지대앞)"}, {"name": "디지털미디어시티"}]}}
                  ]
                }]}
                """, KakaoTransitRouteResponse.class);
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        CommuteResult result = client.findRoute(COORD, COORD, COORD, COORD);

        assertThat(result.getWalkMin()).isEqualTo(1);
        assertThat(result.getTransitMin()).isEqualTo(15);
        assertThat(result.getTransportType()).isEqualTo("SUBWAY");
        assertThat(result.getLineNum()).isEqualTo("6호선");
        assertThat(result.getVehicleType()).isEqualTo("일반");
        assertThat(result.getRoute()).isEqualTo("6호선 증산(명지대앞) → 디지털미디어시티");
    }

    @Test
    void 환승이_있으면_route는_모든_구간을_이어붙이고_lineNum은_첫_구간_기준으로_채운다() throws Exception {
        KakaoTransitRouteResponse response = OBJECT_MAPPER.readValue(
                """
                {"status": "OK", "routes": [{
                  "properties": {"totalTime": 2033, "transfers": 1},
                  "steps": [
                    {"properties": {"type": "BUS", "time": 463, "vehicles": [{"name": "7017", "type": "지선"}], "stops": [{"name": "새마을금고앞"}, {"name": "DMC파인시티자이"}]}},
                    {"properties": {"type": "WALKING", "time": 458, "vehicles": [], "stops": [{"name": "DMC파인시티자이"}, {"name": "수색"}]}},
                    {"properties": {"type": "SUBWAY", "time": 120, "vehicles": [{"name": "경의중앙선", "type": "일반"}], "stops": [{"name": "수색"}, {"name": "디지털미디어시티"}]}}
                  ]
                }]}
                """, KakaoTransitRouteResponse.class);
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        CommuteResult result = client.findRoute(COORD, COORD, COORD, COORD);

        assertThat(result.getWalkMin()).isEqualTo(8);
        assertThat(result.getTransitMin()).isEqualTo(10);
        assertThat(result.getTransportType()).isEqualTo("BUS");
        assertThat(result.getLineNum()).isEqualTo("7017");
        assertThat(result.getVehicleType()).isEqualTo("지선");
        assertThat(result.getRoute()).isEqualTo("7017 새마을금고앞 → DMC파인시티자이, 경의중앙선 수색 → 디지털미디어시티");
    }

    @Test
    void 버스만으로_구성된_경로도_transitMin에_버스_탑승_시간이_반영된다() throws Exception {
        KakaoTransitRouteResponse response = OBJECT_MAPPER.readValue(
                """
                {"status": "OK", "routes": [{
                  "properties": {"totalTime": 1269, "transfers": 0},
                  "steps": [
                    {"properties": {"type": "BUS", "time": 1269, "vehicles": [{"name": "7021", "type": "지선"}], "stops": [{"name": "증산역"}, {"name": "디지털미디어시티역"}]}}
                  ]
                }]}
                """, KakaoTransitRouteResponse.class);
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        CommuteResult result = client.findRoute(COORD, COORD, COORD, COORD);

        assertThat(result.getTransitMin()).isEqualTo(22);
        assertThat(result.getWalkMin()).isNull();
    }

    @Test
    void steps가_없으면_통근_상세_필드는_전부_null이다() throws Exception {
        KakaoTransitRouteResponse response = OBJECT_MAPPER.readValue(
                """
                {"status": "OK", "routes": [{"properties": {"totalTime": 1800, "transfers": 1}}]}
                """, KakaoTransitRouteResponse.class);
        when(restTemplate.exchange(
                any(), eq(HttpMethod.GET), any(), eq(KakaoTransitRouteResponse.class)))
                .thenReturn(ResponseEntity.ok(response));

        CommuteResult result = client.findRoute(COORD, COORD, COORD, COORD);

        assertThat(result.getWalkMin()).isNull();
        assertThat(result.getTransitMin()).isNull();
        assertThat(result.getTransportType()).isNull();
        assertThat(result.getLineNum()).isNull();
        assertThat(result.getVehicleType()).isNull();
        assertThat(result.getRoute()).isNull();
    }

    private HttpClientErrorException tooManyRequests() {
        return HttpClientErrorException.create(
                HttpStatus.TOO_MANY_REQUESTS, "Too Many Requests", new HttpHeaders(), new byte[0], StandardCharsets.UTF_8);
    }
}
