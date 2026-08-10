package com.kbait.anchack.place.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.place.client.KakaoPlaceApiClient;
import com.kbait.anchack.place.client.KakaoPlaceCollectionTargetProvider;
import com.kbait.anchack.place.parser.KakaoPlaceParser;
import com.kbait.anchack.place.validator.KakaoPlaceCategoryValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class PlaceConfig {

    @Bean
    public KakaoPlaceApiProperties kakaoPlaceApiProperties(
            @Value("${kakao.place.rest-api-key:${KAKAO_PLACE_REST_API_KEY:}}") String restApiKey,
            @Value("${kakao.place.radius:${KAKAO_PLACE_RADIUS:7000}}") int radius,
            @Value("${kakao.place.page-size:${KAKAO_PLACE_PAGE_SIZE:15}}") int pageSize,
            @Value("${kakao.place.max-page:${KAKAO_PLACE_MAX_PAGE:45}}") int maxPage,
            @Value("${kakao.place.connect-timeout-ms:${KAKAO_PLACE_CONNECT_TIMEOUT_MS:5000}}") int connectTimeoutMs,
            @Value("${kakao.place.read-timeout-ms:${KAKAO_PLACE_READ_TIMEOUT_MS:30000}}") int readTimeoutMs
    ) {
        return new KakaoPlaceApiProperties(
                restApiKey,
                radius,
                pageSize,
                maxPage,
                connectTimeoutMs,
                readTimeoutMs
        );
    }

    @Bean(name = "kakaoPlaceRestTemplate")
    public RestTemplate kakaoPlaceRestTemplate(KakaoPlaceApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        return new RestTemplate(requestFactory);
    }

    @Bean
    public KakaoPlaceCategoryValidator kakaoPlaceCategoryValidator() {
        return new KakaoPlaceCategoryValidator();
    }

    @Bean
    public KakaoPlaceCollectionTargetProvider kakaoPlaceCollectionTargetProvider() {
        return new KakaoPlaceCollectionTargetProvider();
    }

    @Bean(name = "placeObjectMapper")
    public ObjectMapper placeObjectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public KakaoPlaceParser kakaoPlaceParser(
            @Qualifier("placeObjectMapper") ObjectMapper objectMapper,
            KakaoPlaceCategoryValidator categoryValidator
    ) {
        return new KakaoPlaceParser(objectMapper, categoryValidator);
    }

    @Bean
    public KakaoPlaceApiClient kakaoPlaceApiClient(
            @Qualifier("kakaoPlaceRestTemplate") RestTemplate restTemplate,
            KakaoPlaceApiProperties properties,
            KakaoPlaceParser parser
    ) {
        return new KakaoPlaceApiClient(restTemplate, properties, parser);
    }
}
