package com.kbait.anchack.rental.config;

import com.kbait.anchack.admindong.mapper.AdminDongMapper;
import com.kbait.anchack.place.resolver.AdminDongBoundaryRepository;
import com.kbait.anchack.rental.client.MolitRentApiClient;
import com.kbait.anchack.rental.normalizer.RentalTransactionNormalizer;
import com.kbait.anchack.rental.parser.MolitRentXmlParser;
import com.kbait.anchack.rental.registry.SeoulLawdCodeRegistry;
import com.kbait.anchack.rental.resolver.RentalAdminDongResolver;
import com.kbait.anchack.rental.resolver.RentalAdminDongResolverImpl;
import com.kbait.anchack.route.client.KakaoGeocodingClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
public class MolitRentConfig {

    @Bean
    public MolitRentApiProperties molitRentApiProperties(
            @Value("${molit.api.service-key:${MOLIT_API_SERVICE_KEY:}}") String serviceKey,
            @Value("${molit.api.num-of-rows:${MOLIT_API_NUM_OF_ROWS:100}}") int numOfRows,
            @Value("${molit.api.connect-timeout-ms:${MOLIT_API_CONNECT_TIMEOUT_MS:5000}}") int connectTimeoutMs,
            @Value("${molit.api.read-timeout-ms:${MOLIT_API_READ_TIMEOUT_MS:30000}}") int readTimeoutMs
    ) {
        return new MolitRentApiProperties(
                serviceKey,
                numOfRows,
                connectTimeoutMs,
                readTimeoutMs
        );
    }

    @Bean(name = "molitRestTemplate")
    public RestTemplate molitRestTemplate(MolitRentApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeoutMs());
        requestFactory.setReadTimeout(properties.getReadTimeoutMs());

        return new RestTemplate(requestFactory);
    }

    @Bean
    public MolitRentXmlParser molitRentXmlParser() {
        return new MolitRentXmlParser();
    }

    @Bean
    public RentalTransactionNormalizer rentalTransactionNormalizer() {
        return new RentalTransactionNormalizer();
    }

    @Bean
    public SeoulLawdCodeRegistry seoulLawdCodeRegistry() {
        return new SeoulLawdCodeRegistry();
    }

    @Bean
    public RentalAdminDongResolver rentalAdminDongResolver(
            KakaoGeocodingClient kakaoGeocodingClient,
            AdminDongBoundaryRepository boundaryRepository,
            AdminDongMapper adminDongMapper,
            SeoulLawdCodeRegistry lawdCodeRegistry
    ) {
        return new RentalAdminDongResolverImpl(
                kakaoGeocodingClient,
                boundaryRepository,
                adminDongMapper,
                lawdCodeRegistry
        );
    }

    @Bean
    public MolitRentApiClient molitRentApiClient(
            @Qualifier("molitRestTemplate") RestTemplate molitRestTemplate,
            MolitRentApiProperties properties,
            MolitRentXmlParser parser
    ) {
        return new MolitRentApiClient(molitRestTemplate, properties, parser);
    }
}
