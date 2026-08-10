package com.kbait.anchack.place.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.place.config.KakaoPlaceApiProperties;
import com.kbait.anchack.place.domain.PlaceCategory;
import com.kbait.anchack.place.dto.external.ExternalPlace;
import com.kbait.anchack.place.exception.KakaoPlaceApiException;
import com.kbait.anchack.place.parser.KakaoPlaceParser;
import com.kbait.anchack.place.validator.KakaoPlaceCategoryValidator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.net.URI;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KakaoPlaceApiClientTest {

    private static final String FAKE_REST_API_KEY = "KAKAO_TEST_KEY";
    private static final int RADIUS = 7_000;
    private static final int PAGE_SIZE = 15;
    private static final int MAX_PAGE = 45;

    private MockRestServiceServer server;
    private KakaoPlaceApiClient client;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        server = MockRestServiceServer.bindTo(restTemplate).build();
        client = createClient(restTemplate, FAKE_REST_API_KEY);
    }

    @Test
    void categorySearchRequestsKakaoCategoryApi() {
        PlaceCollectionTarget target = createCategoryTarget();
        server.expect(once(), request -> {
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
                    assertThat(request.getHeaders().getFirst("Authorization"))
                            .isEqualTo("KakaoAK " + FAKE_REST_API_KEY);
                    assertCategoryRequest(request.getURI(), 1);
                })
                .andRespond(withSuccess(responseJson("pharmacy-1", true), MediaType.APPLICATION_JSON));

        List<ExternalPlace> result = client.fetchPlaces(target);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSourcePlaceId()).isEqualTo("pharmacy-1");
        server.verify();
    }

    @Test
    void keywordSearchRequestsKakaoKeywordApi() {
        PlaceCollectionTarget target = createKeywordTarget();
        server.expect(once(), request -> {
                    assertThat(request.getMethod()).isEqualTo(HttpMethod.GET);
                    assertThat(request.getURI().getPath()).isEqualTo("/v2/local/search/keyword.json");
                    assertQueryContains(request.getURI(), "query=river");
                    assertQueryContains(request.getURI(), "x=126.951484");
                    assertQueryContains(request.getURI(), "y=37.478154");
                    assertQueryContains(request.getURI(), "radius=7000");
                    assertQueryContains(request.getURI(), "page=1");
                    assertQueryContains(request.getURI(), "size=15");
                })
                .andRespond(withSuccess(keywordResponseJson("river-1", true), MediaType.APPLICATION_JSON));

        List<ExternalPlace> result = client.fetchPlaces(target);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPlaceCategory()).isEqualTo(PlaceCategory.RIVER);
        server.verify();
    }

    @Test
    void fetchPlacesRequestsNextPageUntilEnd() {
        PlaceCollectionTarget target = createCategoryTarget();
        server.expect(once(), request -> assertCategoryRequest(request.getURI(), 1))
                .andRespond(withSuccess(responseJson("pharmacy-1", false), MediaType.APPLICATION_JSON));
        server.expect(once(), request -> assertCategoryRequest(request.getURI(), 2))
                .andRespond(withSuccess(responseJson("pharmacy-2", true), MediaType.APPLICATION_JSON));

        List<ExternalPlace> result = client.fetchPlaces(target);

        assertThat(result)
                .extracting(ExternalPlace::getSourcePlaceId)
                .containsExactly("pharmacy-1", "pharmacy-2");
        server.verify();
    }

    @Test
    void blankRestApiKeyFailsBeforeHttpRequest() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer emptyKeyServer = MockRestServiceServer.bindTo(restTemplate).build();
        KakaoPlaceApiClient emptyKeyClient = createClient(restTemplate, "");

        Throwable actual = catchThrowable(() -> emptyKeyClient.fetchPlaces(createCategoryTarget()));

        assertThat(actual).isExactlyInstanceOf(KakaoPlaceApiException.class);
        assertSensitiveDataIsAbsent(actual);
        emptyKeyServer.verify();
    }

    @Test
    void httpErrorIsConvertedToKakaoPlaceApiException() {
        PlaceCollectionTarget target = createCategoryTarget();
        server.expect(once(), request -> assertCategoryRequest(request.getURI(), 1))
                .andRespond(withStatus(HttpStatus.UNAUTHORIZED)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body("{\"msg\":\"bad key\"}"));

        Throwable actual = catchThrowable(() -> client.fetchPlaces(target));

        assertThat(actual).isExactlyInstanceOf(KakaoPlaceApiException.class)
                .hasMessageContaining("httpStatus=401")
                .hasNoCause();
        assertSensitiveDataIsAbsent(actual);
        server.verify();
    }

    private KakaoPlaceApiClient createClient(RestTemplate restTemplate, String restApiKey) {
        KakaoPlaceApiProperties properties = new KakaoPlaceApiProperties(
                restApiKey,
                RADIUS,
                PAGE_SIZE,
                MAX_PAGE,
                1_000,
                1_000
        );
        KakaoPlaceParser parser = new KakaoPlaceParser(
                new ObjectMapper(),
                new KakaoPlaceCategoryValidator()
        );

        return new KakaoPlaceApiClient(restTemplate, properties, parser);
    }

    private PlaceCollectionTarget createCategoryTarget() {
        return PlaceCollectionTarget.builder()
                .dataSourceId(13L)
                .dataDate(LocalDate.of(2026, 8, 4))
                .guCode("11620")
                .guName("Gwanak-gu")
                .searchType(KakaoPlaceSearchType.CATEGORY)
                .requestValue("PM9")
                .placeCategory(PlaceCategory.PHARMACY)
                .centerLatitude(new BigDecimal("37.478154"))
                .centerLongitude(new BigDecimal("126.951484"))
                .build();
    }

    private PlaceCollectionTarget createKeywordTarget() {
        return PlaceCollectionTarget.builder()
                .dataSourceId(13L)
                .dataDate(LocalDate.of(2026, 8, 4))
                .guCode("11620")
                .guName("Gwanak-gu")
                .searchType(KakaoPlaceSearchType.KEYWORD)
                .requestValue("river")
                .placeCategory(PlaceCategory.RIVER)
                .expectedCategoryName("travel > attraction > river")
                .centerLatitude(new BigDecimal("37.478154"))
                .centerLongitude(new BigDecimal("126.951484"))
                .build();
    }

    private void assertCategoryRequest(URI uri, int page) {
        assertThat(uri.getPath()).isEqualTo("/v2/local/search/category.json");
        assertQueryContains(uri, "category_group_code=PM9");
        assertQueryContains(uri, "x=126.951484");
        assertQueryContains(uri, "y=37.478154");
        assertQueryContains(uri, "radius=7000");
        assertQueryContains(uri, "page=" + page);
        assertQueryContains(uri, "size=15");
    }

    private void assertQueryContains(URI uri, String queryPart) {
        assertThat(uri.getRawQuery()).contains(queryPart);
    }

    private String responseJson(String id, boolean isEnd) {
        return """
                {
                  "meta": {
                    "total_count": 1,
                    "pageable_count": 1,
                    "is_end": %s
                  },
                  "documents": [
                    {
                      "id": "%s",
                      "place_name": "Safe Pharmacy",
                      "category_group_code": "PM9",
                      "category_name": "medical > pharmacy",
                      "address_name": "Seoul Gwanak-gu Bongcheon-dong",
                      "road_address_name": "Seoul Gwanak-gu Gwanak-ro 1",
                      "x": "126.951484",
                      "y": "37.478154"
                    }
                  ]
                }
                """.formatted(isEnd, id);
    }

    private String keywordResponseJson(String id, boolean isEnd) {
        return """
                {
                  "meta": {
                    "total_count": 1,
                    "pageable_count": 1,
                    "is_end": %s
                  },
                  "documents": [
                    {
                      "id": "%s",
                      "place_name": "Sample River",
                      "category_group_code": "",
                      "category_name": "travel > attraction > river",
                      "address_name": "Seoul Gwanak-gu Bongcheon-dong",
                      "road_address_name": "",
                      "x": "126.951484",
                      "y": "37.478154"
                    }
                  ]
                }
                """.formatted(isEnd, id);
    }

    private void assertSensitiveDataIsAbsent(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current.getMessage() != null) {
                assertThat(current.getMessage()).doesNotContain(
                        FAKE_REST_API_KEY,
                        "KakaoAK",
                        "dapi.kakao.com"
                );
            }
            current = current.getCause();
        }
    }
}
