package com.kbait.anchack.place.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.place.cctv.client.CctvCsvDownloadClient;
import com.kbait.anchack.place.cctv.config.CctvCsvDownloadProperties;
import com.kbait.anchack.place.cctv.converter.PublicCctvPlaceConverter;
import com.kbait.anchack.place.cctv.parser.PublicCctvCsvParser;
import com.kbait.anchack.place.cctv.service.CctvPlaceCollectionService;
import com.kbait.anchack.place.cctv.service.CctvPlaceCollectionServiceImpl;
import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionService;
import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionServiceImpl;
import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionRunner;
import com.kbait.anchack.place.cctv.service.CctvPlaceIngestionRunnerImpl;
import com.kbait.anchack.place.client.KakaoPlaceApiClient;
import com.kbait.anchack.place.client.KakaoPlaceCollectionTargetProvider;
import com.kbait.anchack.place.mapper.PlaceAdminDongMapper;
import com.kbait.anchack.place.normalizer.PlaceNormalizer;
import com.kbait.anchack.place.parser.KakaoPlaceParser;
import com.kbait.anchack.place.resolver.AdminDongBoundaryRepository;
import com.kbait.anchack.place.resolver.GeoJsonAdminDongBoundaryRepository;
import com.kbait.anchack.place.resolver.PlaceAdminDongResolver;
import com.kbait.anchack.place.resolver.PlaceAdminDongResolverImpl;
import com.kbait.anchack.place.service.PlaceWriteService;
import com.kbait.anchack.place.validator.KakaoPlaceCategoryValidator;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class PlaceConfig {

    @Bean
    public CctvCsvDownloadProperties cctvCsvDownloadProperties(
            @Value("${cctv.csv.validation-url:https://file.localdata.go.kr/file/validate/download-count}") String validationUrl,
            @Value("${cctv.csv.download-url:https://file.localdata.go.kr/file/download/cctv_info/info?orgCode=6110000_ALL}") String downloadUrl,
            @Value("${cctv.csv.referer:https://file.localdata.go.kr/file/cctv_info/info}") String referer,
            @Value("${cctv.csv.user-agent:Anchack-CCTV-Collector/1.0}") String userAgent,
            @Value("${cctv.csv.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${cctv.csv.request-timeout-ms:30000}") int requestTimeoutMs
    ) {
        return new CctvCsvDownloadProperties(
                validationUrl,
                downloadUrl,
                referer,
                userAgent,
                connectTimeoutMs,
                requestTimeoutMs
        );
    }

    @Bean(name = "cctvCsvHttpClient")
    public HttpClient cctvCsvHttpClient(CctvCsvDownloadProperties properties) {
        CookieManager cookieManager = new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER);

        return HttpClient.newBuilder()
                .cookieHandler(cookieManager)
                .connectTimeout(Duration.ofMillis(properties.getConnectTimeoutMs()))
                .build();
    }

    @Bean
    public CctvCsvDownloadClient cctvCsvDownloadClient(
            @Qualifier("cctvCsvHttpClient") HttpClient httpClient,
            CctvCsvDownloadProperties properties
    ) {
        return new CctvCsvDownloadClient(httpClient, properties);
    }

    @Bean
    public PublicCctvCsvParser publicCctvCsvParser() {
        return new PublicCctvCsvParser();
    }

    @Bean
    public PublicCctvPlaceConverter publicCctvPlaceConverter() {
        return new PublicCctvPlaceConverter();
    }

    @Bean
    public CctvPlaceCollectionService cctvPlaceCollectionService(
            PublicCctvCsvParser csvParser,
            PublicCctvPlaceConverter placeConverter
    ) {
        return new CctvPlaceCollectionServiceImpl(csvParser, placeConverter);
    }

    @Bean
    public CctvPlaceIngestionService cctvPlaceIngestionService(
            CctvPlaceCollectionService collectionService,
            PlaceNormalizer placeNormalizer,
            PlaceAdminDongResolver placeAdminDongResolver,
            PlaceWriteService placeWriteService
    ) {
        return new CctvPlaceIngestionServiceImpl(
                collectionService,
                placeNormalizer,
                placeAdminDongResolver,
                placeWriteService
        );
    }

    @Bean
    public CctvPlaceIngestionRunner cctvPlaceIngestionRunner(
            CctvCsvDownloadClient csvDownloadClient,
            CctvPlaceIngestionService ingestionService
    ) {
        return new CctvPlaceIngestionRunnerImpl(csvDownloadClient, ingestionService);
    }

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

    @Bean
    public PlaceNormalizer placeNormalizer() {
        return new PlaceNormalizer();
    }

    @Bean
    public AdminDongBoundaryRepository adminDongBoundaryRepository(
            @Qualifier("placeObjectMapper") ObjectMapper objectMapper
    ) {
        return new GeoJsonAdminDongBoundaryRepository(objectMapper);
    }

    @Bean
    public PlaceAdminDongResolver placeAdminDongResolver(
            PlaceAdminDongMapper placeAdminDongMapper,
            AdminDongBoundaryRepository boundaryRepository
    ) {
        return new PlaceAdminDongResolverImpl(placeAdminDongMapper, boundaryRepository);
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
