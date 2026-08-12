package com.kbait.anchack.route.config;

import com.kbait.anchack.route.client.KakaoGeocodingClient;
import com.kbait.anchack.route.client.KakaoTransitDirectionsClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RouteConfig {

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
}
