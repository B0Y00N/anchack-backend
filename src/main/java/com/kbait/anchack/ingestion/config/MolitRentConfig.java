package com.kbait.anchack.ingestion.config;

import com.kbait.anchack.ingestion.client.MolitRentApiClient;
import com.kbait.anchack.ingestion.parser.MolitRentXmlParser;
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
    public MolitRentApiClient molitRentApiClient(
            @Qualifier("molitRestTemplate") RestTemplate molitRestTemplate,
            MolitRentApiProperties properties,
            MolitRentXmlParser parser
    ) {
        return new MolitRentApiClient(molitRestTemplate, properties, parser);
    }
}
