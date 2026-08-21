package com.kbait.anchack.route.config;

import com.kbait.anchack.route.client.KakaoGeocodingClient;
import com.kbait.anchack.route.client.KakaoTransitDirectionsClient;
import com.kbait.anchack.route.service.RouteService;
import com.kbait.anchack.route.service.impl.RouteServiceImpl;
import com.kbait.anchack.route.service.impl.StubRouteService;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RouteConfig {

    private static final String MODE_STUB = "stub";

    @Bean
    public RouteApiProperties routeApiProperties(
            @Value("${kakao.route.rest-api-key:${KAKAO_ROUTE_REST_API_KEY:}}") String restApiKey,
            @Value("${kakao.route.connect-timeout-ms:${KAKAO_ROUTE_CONNECT_TIMEOUT_MS:5000}}") int connectTimeoutMs,
            @Value("${kakao.route.read-timeout-ms:${KAKAO_ROUTE_READ_TIMEOUT_MS:5000}}") int readTimeoutMs
    ) {
        return new RouteApiProperties(restApiKey, connectTimeoutMs, readTimeoutMs);
    }

    @Bean(name = "kakaoRouteRestTemplate")
    public RestTemplate kakaoRouteRestTemplate(RouteApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        return new RestTemplate(requestFactory);
    }

    @Bean
    public KakaoGeocodingClient kakaoGeocodingClient(
            @Qualifier("kakaoRouteRestTemplate") RestTemplate restTemplate,
            RouteApiProperties properties
    ) {
        return new KakaoGeocodingClient(restTemplate, properties);
    }

    @Bean
    public KakaoTransitDirectionsClient kakaoTransitDirectionsClient(
            @Qualifier("kakaoRouteRestTemplate") RestTemplate restTemplate,
            RouteApiProperties properties
    ) {
        return new KakaoTransitDirectionsClient(restTemplate, properties);
    }

    /**
     * route.mode(기본 kakao)로 실제 카카오 호출과 스텁을 코드 변경 없이 전환한다.
     * k6 부하테스트에서 destAddress를 채운 시나리오를 돌릴 때 카카오 쪽 12.5 req/s
     * 전역 페이싱(KakaoTransitDirectionsClient 참고)에 막히지 않고 하드필터 이후
     * 파이프라인(스코어링/DB)만 측정하려면 .env에 ROUTE_MODE=stub으로 재기동한다.
     */
    @Bean
    public RouteService routeService(
            @Value("${route.mode:${ROUTE_MODE:kakao}}") String mode,
            KakaoGeocodingClient kakaoGeocodingClient,
            KakaoTransitDirectionsClient kakaoTransitDirectionsClient
    ) {
        if (MODE_STUB.equalsIgnoreCase(mode)) {
            return new StubRouteService();
        }

        return new RouteServiceImpl(kakaoGeocodingClient, kakaoTransitDirectionsClient);
    }
}
