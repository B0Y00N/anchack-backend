package com.kbait.anchack.route.client;

import com.kbait.anchack.route.config.RouteApiProperties;
import com.kbait.anchack.route.dto.CommuteResult;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRoute;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRouteProperties;
import com.kbait.anchack.route.dto.kakao.KakaoTransitRouteResponse;
import com.kbait.anchack.route.exception.KakaoRouteApiException;
import com.kbait.anchack.route.exception.RouteNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;
import java.util.Objects;

/**
 * 카카오 대중교통 길찾기(publictraffic). place 도메인의 KakaoPlaceApiClient와 동일한 구조.
 *
 * {@code CommuteFilter}가 후보 수백 건을 고정 스레드풀로 지속적으로 병렬 호출하다 보니
 * 카카오 쪽에서 간헐적으로 429(TooManyRequests, 순간 레이트리밋)를 반환하는 걸 확인했다
 * (CommuteFilter의 과거 TODO 참고). 429는 "지금은 안 되니 잠시 후 다시 시도해라"라는
 * 명시적 신호이므로, 짧게 대기 후 한 번만 재시도한다 - 재시도로도 안 되면 그대로
 * KakaoRouteApiException으로 전파해 CommuteFilter가 해당 후보만 통근 정보 없이
 * 소프트 처리하도록 맡긴다.
 */
public final class KakaoTransitDirectionsClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoTransitDirectionsClient.class);

    private static final String DIRECTIONS_ENDPOINT = "https://dapi.kakao.com/v2/routing/publictraffic";
    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";
    private static final String STATUS_OK = "OK";
    private static final long RATE_LIMIT_RETRY_DELAY_MS = 500;

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

        KakaoTransitRouteResponse response =
                fetch(originLatitude, originLongitude, destLatitude, destLongitude, true);

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
            BigDecimal destLongitude,
            boolean allowRetryOnRateLimit
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
        } catch (HttpClientErrorException.TooManyRequests exception) {
            if (!allowRetryOnRateLimit) {
                throw new KakaoRouteApiException(
                        "카카오 대중교통 길찾기 API 호출 실패: 재시도 후에도 429(TooManyRequests)", exception);
            }

            log.warn("카카오 대중교통 길찾기 429(TooManyRequests) - {}ms 대기 후 1회 재시도",
                    RATE_LIMIT_RETRY_DELAY_MS);
            sleepBeforeRetry();

            return fetch(originLatitude, originLongitude, destLatitude, destLongitude, false);
        } catch (RestClientResponseException exception) {
            throw new KakaoRouteApiException(
                    "카카오 대중교통 길찾기 API 호출 실패: httpStatus=" + exception.getRawStatusCode()
                            + ", body=" + exception.getResponseBodyAsString(),
                    exception);
        } catch (RestClientException exception) {
            throw new KakaoRouteApiException("카카오 대중교통 길찾기 API 호출 실패", exception);
        }
    }

    private void sleepBeforeRetry() {
        try {
            Thread.sleep(RATE_LIMIT_RETRY_DELAY_MS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new KakaoRouteApiException("429 재시도 대기 중 인터럽트됨", exception);
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
