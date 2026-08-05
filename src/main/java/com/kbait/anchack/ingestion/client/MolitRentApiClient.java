package com.kbait.anchack.ingestion.client;

import com.kbait.anchack.ingestion.config.MolitRentApiProperties;
import com.kbait.anchack.ingestion.dto.external.MolitRentPage;
import com.kbait.anchack.ingestion.dto.external.RawRentalTransaction;
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
import java.util.ArrayList;
import java.util.List;
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

    public List<RawRentalTransaction> fetchAllPages(
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        MolitRentPage firstPage = fetchPage(apiCategory, guCode, dealYearMonth, 1);
        validateFirstPage(firstPage, apiCategory, guCode, dealYearMonth);

        List<RawRentalTransaction> transactions = new ArrayList<>(firstPage.getItems());
        validateAccumulatedItemCount(transactions.size(), firstPage.getTotalCount(), apiCategory, guCode,
                dealYearMonth, 1);

        if (firstPage.getTotalCount() == 0) {
            return List.copyOf(transactions);
        }

        long totalPages = calculateTotalPages(firstPage.getTotalCount(), firstPage.getNumOfRows());
        fetchRemainingPages(firstPage, totalPages, transactions, apiCategory, guCode, dealYearMonth);
        validateFinalItemCount(transactions.size(), firstPage.getTotalCount(), totalPages, apiCategory, guCode,
                dealYearMonth);

        return List.copyOf(transactions);
    }

    private long calculateTotalPages(int totalCount, int numOfRows) {
        return ((long) totalCount + numOfRows - 1L) / numOfRows;
    }

    private void fetchRemainingPages(
            MolitRentPage firstPage,
            long totalPages,
            List<RawRentalTransaction> transactions,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        for (long pageNumber = 2L; pageNumber <= totalPages; pageNumber++) {
            int requestedPage = Math.toIntExact(pageNumber);
            MolitRentPage page = fetchPage(apiCategory, guCode, dealYearMonth, requestedPage);
            validateFollowingPage(page, firstPage, requestedPage, totalPages, apiCategory, guCode, dealYearMonth);
            transactions.addAll(page.getItems());
            validateAccumulatedItemCount(transactions.size(), firstPage.getTotalCount(), apiCategory, guCode,
                    dealYearMonth, requestedPage);
        }
    }

    private void validateFirstPage(
            MolitRentPage page,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        validateResponsePageNumber(page, 1, apiCategory, guCode, dealYearMonth);

        if (page.getNumOfRows() < 1) {
            throw createPageException(apiCategory, guCode, dealYearMonth, 1,
                    "numOfRows=" + page.getNumOfRows() + ", minimumNumOfRows=1");
        }
        if (page.getTotalCount() < 0) {
            throw createPageException(apiCategory, guCode, dealYearMonth, 1,
                    "totalCount=" + page.getTotalCount() + ", minimumTotalCount=0");
        }

        validatePageItemCount(page, 1, apiCategory, guCode, dealYearMonth);
        validateFirstPageItems(page, apiCategory, guCode, dealYearMonth);
    }

    private void validateFirstPageItems(
            MolitRentPage page,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        if (page.getTotalCount() == 0 && !page.getItems().isEmpty()) {
            throw createPageException(apiCategory, guCode, dealYearMonth, 1,
                    "totalCount=0, itemCount=" + page.getItems().size());
        }
        if (page.getTotalCount() > 0 && page.getItems().isEmpty()) {
            throw createPageException(apiCategory, guCode, dealYearMonth, 1,
                    "totalCount=" + page.getTotalCount() + ", itemCount=0");
        }
    }

    private void validateFollowingPage(
            MolitRentPage page,
            MolitRentPage firstPage,
            int requestedPage,
            long totalPages,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        validateResponsePageNumber(page, requestedPage, apiCategory, guCode, dealYearMonth);
        validateFollowingPageMetadata(page, firstPage, requestedPage, apiCategory, guCode, dealYearMonth);
        validatePageItemCount(page, requestedPage, apiCategory, guCode, dealYearMonth);

        if ((long) requestedPage < totalPages && page.getItems().isEmpty()) {
            throw createPageException(apiCategory, guCode, dealYearMonth, requestedPage, "itemCount=0");
        }
    }

    private void validateFollowingPageMetadata(
            MolitRentPage page,
            MolitRentPage firstPage,
            int requestedPage,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        if (page.getNumOfRows() != firstPage.getNumOfRows()) {
            throw createPageException(apiCategory, guCode, dealYearMonth, requestedPage,
                    "numOfRows=" + page.getNumOfRows() + ", expectedNumOfRows=" + firstPage.getNumOfRows());
        }
        if (page.getTotalCount() != firstPage.getTotalCount()) {
            throw createPageException(apiCategory, guCode, dealYearMonth, requestedPage,
                    "totalCount=" + page.getTotalCount() + ", expectedTotalCount=" + firstPage.getTotalCount());
        }
    }

    private void validateResponsePageNumber(
            MolitRentPage page,
            int requestedPage,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        if (page.getPageNo() != requestedPage) {
            throw createPageException(apiCategory, guCode, dealYearMonth, requestedPage,
                    "responsePageNo=" + page.getPageNo());
        }
    }

    private void validatePageItemCount(
            MolitRentPage page,
            int requestedPage,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        if (page.getItems().size() > page.getNumOfRows()) {
            throw createPageException(apiCategory, guCode, dealYearMonth, requestedPage,
                    "itemCount=" + page.getItems().size() + ", numOfRows=" + page.getNumOfRows());
        }
    }

    private void validateAccumulatedItemCount(
            int accumulatedItemCount,
            int totalCount,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            int requestedPage
    ) {
        if (accumulatedItemCount > totalCount) {
            throw createPageException(apiCategory, guCode, dealYearMonth, requestedPage,
                    "accumulatedItemCount=" + accumulatedItemCount + ", totalCount=" + totalCount);
        }
    }

    private void validateFinalItemCount(
            int accumulatedItemCount,
            int totalCount,
            long totalPages,
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth
    ) {
        if (accumulatedItemCount != totalCount) {
            throw createPageException(apiCategory, guCode, dealYearMonth, totalPages,
                    "accumulatedItemCount=" + accumulatedItemCount + ", totalCount=" + totalCount);
        }
    }

    private MolitRentApiException createPageException(
            MolitRentApiCategory apiCategory,
            String guCode,
            YearMonth dealYearMonth,
            long requestedPage,
            String mismatch
    ) {
        return new MolitRentApiException(
                "국토부 API 페이지 응답 불일치: apiCategory=" + apiCategory
                        + ", guCode=" + guCode
                        + ", dealYearMonth=" + dealYearMonth
                        + ", requestedPage=" + requestedPage
                        + ", " + mismatch
        );
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
