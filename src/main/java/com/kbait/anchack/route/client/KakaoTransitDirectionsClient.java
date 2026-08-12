package com.kbait.anchack.route.client;

import com.kbait.anchack.route.config.RouteApiProperties;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRoute;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRouteProperties;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRouteResponse;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import com.kbait.anchack.route.exception.RouteNotFoundException;
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

/** 카카오 대중교통 길찾기(publictraffic). place 도메인의 KakaoPlaceApiClient와 동일한 구조. */
public final class KakaoTransitDirectionsClient {

    private static final String DIRECTIONS_ENDPOINT = "https://dapi.kakao.com/v2/routing/publictraffic";
    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";
    private static final String STATUS_OK = "OK";

    private final RestTemplate restTemplate;
    private final RouteApiProperties properties;

    public KakaoTransitDirectionsClient(RestTemplate restTemplate, RouteApiProperties properties) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate는 null일 수 없습니다.");
        this.properties = Objects.requireNonNull(properties, "properties는 null일 수 없습니다.");
    }

    public CommuteResult findRoute(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            BigDecimal destLatitude,
            BigDecimal destLongitude
    ) {
        validateRestApiKey();

        KakaoTransitRouteResponse response = fetch(originLatitude, originLongitude, destLatitude, destLongitude);

        if (!STATUS_OK.equals(response.getStatus())) {
            throw new RouteNotFoundException(
                    "대중교통 경로를 찾을 수 없습니다: status=" + response.getStatus());
        }

        return toCommuteResult(response.getRoutes());
    }

    private KakaoTransitRouteResponse fetch(
            BigDecimal originLatitude,
            BigDecimal originLongitude,
            BigDecimal destLatitude,
            BigDecimal destLongitude
    ) {
        URI uri = UriComponentsBuilder.fromHttpUrl(DIRECTIONS_ENDPOINT)
                .queryParam("start_x", originLongitude.toPlainString())
                .queryParam("start_y", originLatitude.toPlainString())
                .queryParam("end_x", destLongitude.toPlainString())
                .queryParam("end_y", destLatitude.toPlainString())
                .build()
                .encode()
                .toUri();

        HttpEntity<Void> request = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<KakaoTransitRouteResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, request, KakaoTransitRouteResponse.class);

            return response.getBody();
        } catch (RestClientResponseException exception) {
            throw new KakaoRouteApiException(
                    "카카오 대중교통 길찾기 API 호출 실패: httpStatus=" + exception.getRawStatusCode()
                            + ", body=" + exception.getResponseBodyAsString(),
                    exception);
        } catch (RestClientException exception) {
            throw new KakaoRouteApiException("카카오 대중교통 길찾기 API 호출 실패", exception);
        }
    }

    private CommuteResult toCommuteResult(List<KakaoTransitRoute> routes) {
        if (routes.isEmpty() || routes.get(0).getProperties() == null) {
            throw new RouteNotFoundException("대중교통 경로를 찾을 수 없습니다: routes가 비어 있습니다.");
        }

        KakaoTransitRouteProperties routeProperties = routes.get(0).getProperties();

        return CommuteResult.builder()
                .commuteTime(secondsToMinutes(routeProperties.getTotalTime()))
                .transferCount(routeProperties.getTransfers())
                .build();
    }

    private Integer secondsToMinutes(Integer seconds) {
        return seconds == null ? null : (int) Math.ceil(seconds / 60.0);
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
