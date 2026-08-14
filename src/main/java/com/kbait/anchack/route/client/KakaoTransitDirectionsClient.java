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
import java.util.concurrent.atomic.AtomicLong;

/**
 * 카카오 대중교통 길찾기(publictraffic). place 도메인의 KakaoPlaceApiClient와 동일한 구조.
 *
 * {@code CommuteFilter}가 후보 수백 건을 고정 스레드풀로 지속적으로 병렬 호출하다 보니
 * 카카오 쪽에서 간헐적으로 429(TooManyRequests, 순간 레이트리밋)를 반환하는 걸 확인했다
 * (CommuteFilter의 과거 TODO 참고). 429는 "지금은 안 되니 잠시 후 다시 시도해라"라는
 * 명시적 신호이므로, 짧게 대기 후 한 번만 재시도한다 - 재시도로도 안 되면 그대로
 * KakaoRouteApiException으로 전파해 CommuteFilter가 해당 후보만 통근 정보 없이
 * 소프트 처리하도록 맡긴다.
 *
 * 실측 결과 대중교통 경로 API는 현재 환경에서 약 20 req/s 부근의 단기(≈1초 window) 호출
 * 제한이 관찰되었으므로, 안전 마진을 두어 {@link #paceGlobally()}로 애플리케이션 전체
 * 호출을 약 12.5 req/s(80ms 간격)로 페이싱한다. 이 클라이언트는 Spring 싱글턴 빈이라
 * {@code nextAvailableCallMillis}가 요청 1건이 아니라 애플리케이션 전체에서 동시에 들어오는
 * 모든 CommuteFilter 스레드/요청 사이에서 공유된다.
 */
public final class KakaoTransitDirectionsClient {

    private static final Logger log = LoggerFactory.getLogger(KakaoTransitDirectionsClient.class);

    private static final String DIRECTIONS_ENDPOINT = "https://dapi.kakao.com/v2/routing/publictraffic";
    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";
    private static final String STATUS_OK = "OK";
    private static final long RATE_LIMIT_RETRY_DELAY_MS = 500;
    private static final long PACING_INTERVAL_MS = 80;

    private final RestTemplate restTemplate;
    private final RouteApiProperties properties;
    private final AtomicLong nextAvailableCallMillis = new AtomicLong(System.currentTimeMillis());

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
        paceGlobally();

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

    /**
     * 다음 호출 슬롯을 CAS로 예약해 애플리케이션 전체 호출 간격을 최소 {@link #PACING_INTERVAL_MS}ms로
     * 강제한다. 동시에 여러 스레드가 진입해도 각자 서로 다른 슬롯을 받아가므로 전체 합계 호출량이
     * 12.5 req/s를 넘지 않는다.
     */
    private void paceGlobally() {
        long myTurn;

        while (true) {
            long current = nextAvailableCallMillis.get();
            long now = System.currentTimeMillis();
            myTurn = Math.max(current, now);

            if (nextAvailableCallMillis.compareAndSet(current, myTurn + PACING_INTERVAL_MS)) {
                break;
            }
        }

        long waitMillis = myTurn - System.currentTimeMillis();

        if (waitMillis <= 0) {
            return;
        }

        try {
            Thread.sleep(waitMillis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new KakaoRouteApiException("페이싱 대기 중 인터럽트됨", e);
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
