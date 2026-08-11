package com.kbait.anchack.place.client;

import com.kbait.anchack.place.config.KakaoPlaceApiProperties;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.dto.external.KakaoPlacePage;
import com.kbait.anchack.place.exception.KakaoPlaceApiException;
import com.kbait.anchack.place.parser.KakaoPlaceParser;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KakaoPlaceApiClient {

    private static final String CATEGORY_SEARCH_ENDPOINT =
            "https://dapi.kakao.com/v2/local/search/category.json";
    private static final String KEYWORD_SEARCH_ENDPOINT =
            "https://dapi.kakao.com/v2/local/search/keyword.json";
    private static final String AUTHORIZATION_PREFIX = "KakaoAK ";

    private final RestTemplate restTemplate;
    private final KakaoPlaceApiProperties properties;
    private final KakaoPlaceParser parser;

    public KakaoPlaceApiClient(
            RestTemplate restTemplate,
            KakaoPlaceApiProperties properties,
            KakaoPlaceParser parser
    ) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate는 null일 수 없습니다.");
        this.properties = Objects.requireNonNull(properties, "properties는 null일 수 없습니다.");
        this.parser = Objects.requireNonNull(parser, "parser는 null일 수 없습니다.");
    }

    public List<ExternalPlace> fetchPlaces(PlaceCollectionTarget target) {
        validateTarget(target);
        validateRestApiKey();

        List<ExternalPlace> places = new ArrayList<>();
        for (int page = 1; page <= properties.getMaxPage(); page++) {
            KakaoPlacePage kakaoPlacePage = fetchPage(target, page);
            places.addAll(kakaoPlacePage.getPlaces());

            if (kakaoPlacePage.isEnd()) {
                return List.copyOf(places);
            }
        }

        return List.copyOf(places);
    }

    private KakaoPlacePage fetchPage(
            PlaceCollectionTarget target,
            int page
    ) {
        URI uri = createUri(target, page);
        HttpEntity<Void> request = new HttpEntity<>(createHeaders());

        try {
            ResponseEntity<String> response = restTemplate.exchange(
                    uri,
                    HttpMethod.GET,
                    request,
                    String.class
            );
            return parser.parse(response.getBody(), target);
        } catch (RestClientResponseException exception) {
            throw createHttpException(target, page, exception.getRawStatusCode());
        } catch (RestClientException exception) {
            throw createCallException(target, page);
        }
    }

    private URI createUri(
            PlaceCollectionTarget target,
            int page
    ) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getEndpoint(target))
                .queryParam("x", target.getCenterLongitude())
                .queryParam("y", target.getCenterLatitude())
                .queryParam("radius", properties.getRadius())
                .queryParam("page", page)
                .queryParam("size", properties.getPageSize());

        if (target.getSearchType() == KakaoPlaceSearchType.CATEGORY) {
            builder.queryParam("category_group_code", target.getRequestValue());
        } else {
            builder.queryParam("query", target.getRequestValue());
        }

        return builder.build()
                .encode()
                .toUri();
    }

    private String getEndpoint(PlaceCollectionTarget target) {
        if (target.getSearchType() == KakaoPlaceSearchType.CATEGORY) {
            return CATEGORY_SEARCH_ENDPOINT;
        }

        return KEYWORD_SEARCH_ENDPOINT;
    }

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, AUTHORIZATION_PREFIX + properties.getRestApiKey());
        return headers;
    }

    private void validateRestApiKey() {
        if (!StringUtils.hasText(properties.getRestApiKey())) {
            throw new KakaoPlaceApiException("카카오 장소 API 키가 설정되지 않았습니다.");
        }
    }

    private void validateTarget(PlaceCollectionTarget target) {
        Objects.requireNonNull(target, "target은 null일 수 없습니다.");
        Objects.requireNonNull(target.getSearchType(), "searchType은 null일 수 없습니다.");
        Objects.requireNonNull(target.getCenterLatitude(), "centerLatitude는 null일 수 없습니다.");
        Objects.requireNonNull(target.getCenterLongitude(), "centerLongitude는 null일 수 없습니다.");

        if (!StringUtils.hasText(target.getRequestValue())) {
            throw new IllegalArgumentException("requestValue는 비어 있을 수 없습니다.");
        }
    }

    private KakaoPlaceApiException createHttpException(
            PlaceCollectionTarget target,
            int page,
            int httpStatus
    ) {
        return new KakaoPlaceApiException(
                "카카오 장소 API 호출 실패: guCode=" + target.getGuCode()
                        + ", searchType=" + target.getSearchType()
                        + ", requestValue=" + target.getRequestValue()
                        + ", page=" + page
                        + ", httpStatus=" + httpStatus
        );
    }

    private KakaoPlaceApiException createCallException(
            PlaceCollectionTarget target,
            int page
    ) {
        return new KakaoPlaceApiException(
                "카카오 장소 API 호출 실패: guCode=" + target.getGuCode()
                        + ", searchType=" + target.getSearchType()
                        + ", requestValue=" + target.getRequestValue()
                        + ", page=" + page
        );
    }
}
