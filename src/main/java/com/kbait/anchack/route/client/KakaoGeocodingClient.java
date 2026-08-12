package com.kbait.anchack.route.client;

import com.kbait.anchack.route.config.RouteApiProperties;
import com.kbait.anchack.route.dto.Coordinates;
import com.kbait.anchack.route.dto.kakao.KakaoAddressDocument;
import com.kbait.anchack.route.dto.kakao.KakaoAddressSearchResponse;
import com.kbait.anchack.route.exception.AddressNotFoundException;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/** 카카오 주소 검색(geocoding). place 도메인의 KakaoPlaceApiClient와 동일한 구조. */
public final class KakaoGeocodingClient {

    private static final String ADDRESS_SEARCH_ENDPOINT = "https://dapi.kakao.com/v2/local/search/address.json";
    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

    private final RestTemplate restTemplate;
    private final RouteApiProperties properties;

    public KakaoGeocodingClient(RestTemplate restTemplate, RouteApiProperties properties) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate는 null일 수 없습니다.");
        this.properties = Objects.requireNonNull(properties, "properties는 null일 수 없습니다.");
    }

    public Coordinates geocode(String address) {
        validateRestApiKey();

        KakaoAddressSearchResponse response = fetch(address);
        List<KakaoAddressDocument> documents = response.getDocuments();

        if (documents.isEmpty()) {
            throw new AddressNotFoundException("주소를 찾을 수 없습니다: " + address);
        }

        KakaoAddressDocument document = documents.get(0);

        return Coordinates.builder()
                .longitude(new BigDecimal(document.getX()))
                .latitude(new BigDecimal(document.getY()))
                .build();
    }

    private KakaoAddressSearchResponse fetch(String address) {
        URI uri = UriComponentsBuilder.fromHttpUrl(ADDRESS_SEARCH_ENDPOINT)
                .queryParam("query", address)
                .build()
                .encode()
                .toUri();

        HttpEntity<Void> request = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<KakaoAddressSearchResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, request, KakaoAddressSearchResponse.class);

            return response.getBody();
        } catch (RestClientResponseException exception) {
            throw new KakaoRouteApiException(
                    "카카오 주소 검색 API 호출 실패: address=" + address
                            + ", httpStatus=" + exception.getRawStatusCode(),
                    exception);
        } catch (RestClientException exception) {
            throw new KakaoRouteApiException(
                    "카카오 주소 검색 API 호출 실패: address=" + address, exception);
        }
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + properties.getRestApiKey());

        return headers;
    }

    private void validateRestApiKey() {
        if (!StringUtils.hasText(properties.getRestApiKey())) {
            throw new KakaoRouteApiException("카카오 길찾기 API 키가 설정되지 않았습니다.");
        }
    }
}
