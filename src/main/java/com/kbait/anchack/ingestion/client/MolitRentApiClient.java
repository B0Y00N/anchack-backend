package com.kbait.anchack.ingestion.client;

import com.kbait.anchack.ingestion.config.MolitRentApiProperties;
import com.kbait.anchack.ingestion.dto.external.MolitRentPage;
import com.kbait.anchack.ingestion.exception.MolitRentApiException;
import com.kbait.anchack.ingestion.exception.MolitRentApiResponseException;
import com.kbait.anchack.ingestion.exception.MolitRentParseException;
import com.kbait.anchack.ingestion.parser.MolitRentXmlParser;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.regex.Pattern;

public final class MolitRentApiClient {

    private static final Pattern GU_CODE_PATTERN = Pattern.compile("[0-9]{5}");
    private static final DateTimeFormatter DEAL_YEAR_MONTH_FORMATTER = DateTimeFormatter.ofPattern("yyyyMM");

    private final RestTemplate restTemplate;
    private final MolitRentApiProperties properties;
    private final MolitRentXmlParser parser;

    public MolitRentApiClient(
            RestTemplate restTemplate,
            MolitRentApiProperties properties,
            MolitRentXmlParser parser
    ) {
        this.restTemplate = Objects.requireNonNull(restTemplate, "restTemplate은 null일 수 없습니다.");
        this.properties = Objects.requireNonNull(properties, "properties는 null일 수 없습니다.");
        this.parser = Objects.requireNonNull(parser, "parser는 null일 수 없습니다.");
    }

    public MolitRentPage fetchPage(
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            int pageNo
    ) {
        validateRequest(apiCategory, guCode, dealYearMonth, pageNo);
        String serviceKey = getServiceKey();
        URI uri = createUri(apiCategory, guCode, dealYearMonth, pageNo, serviceKey);
        String xml = fetchResponse(uri, apiCategory, guCode, dealYearMonth, pageNo);

        return parser.parse(apiCategory, xml);
    }

    private void validateRequest(
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            int pageNo
    ) {
        Objects.requireNonNull(apiCategory, "apiCategory는 null일 수 없습니다.");
        Objects.requireNonNull(dealYearMonth, "dealYearMonth는 null일 수 없습니다.");

        if (guCode == null || !GU_CODE_PATTERN.matcher(guCode).matches()) {
            throw new IllegalArgumentException("guCode는 숫자 5자리여야 합니다.");
        }

        if (pageNo < 1) {
            throw new IllegalArgumentException("pageNo는 1 이상이어야 합니다.");
        }
    }

    private String getServiceKey() {
        String serviceKey = properties.getServiceKey();

        if (serviceKey == null || serviceKey.isBlank()) {
            throw new MolitRentApiException("국토부 API 서비스 키가 설정되지 않았습니다.");
        }

        return serviceKey;
    }

    private URI createUri(
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            int pageNo,
            String serviceKey
    ) {
        String encodedServiceKey = URLEncoder.encode(serviceKey, StandardCharsets.UTF_8);

        return UriComponentsBuilder.fromHttpUrl(apiCategory.getEndpoint())
                .queryParam("serviceKey", encodedServiceKey)
                .queryParam("LAWD_CD", guCode)
                .queryParam("DEAL_YMD", dealYearMonth.format(DEAL_YEAR_MONTH_FORMATTER))
                .queryParam("pageNo", pageNo)
                .queryParam("numOfRows", properties.getNumOfRows())
                .build(true)
                .toUri();
    }

    private String fetchResponse(
            URI uri,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            int pageNo
    ) {
        try {
            return restTemplate.getForObject(uri, String.class);
        } catch (RestClientResponseException exception) {
            throw parseHttpError(exception, apiCategory, guCode, dealYearMonth, pageNo);
        } catch (RestClientException exception) {
            throw createCallException(apiCategory, guCode, dealYearMonth, pageNo, null);
        }
    }

    private MolitRentApiException parseHttpError(
            RestClientResponseException exception,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            int pageNo
    ) {
        String responseBody = exception.getResponseBodyAsString(StandardCharsets.UTF_8);

        try {
            parser.parse(apiCategory, responseBody);
        } catch (MolitRentApiResponseException apiResponseException) {
            return apiResponseException;
        } catch (MolitRentParseException parseException) {
            return createCallException(
                    apiCategory,
                    guCode,
                    dealYearMonth,
                    pageNo,
                    exception.getRawStatusCode()
            );
        }

        return createCallException(
                apiCategory,
                guCode,
                dealYearMonth,
                pageNo,
                exception.getRawStatusCode()
        );
    }

    private MolitRentApiException createCallException(
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            int pageNo,
            Integer httpStatus
    ) {
        String message = "국토부 API 단일 페이지 호출 실패: apiCategory=" + apiCategory
                + ", guCode=" + guCode
                + ", dealYearMonth=" + dealYearMonth
                + ", pageNo=" + pageNo;

        if (httpStatus != null) {
            message += ", httpStatus=" + httpStatus;
        }

        return new MolitRentApiException(message);
    }
}
