package com.kbait.anchack.place.cctv.client;

import com.kbait.anchack.place.cctv.config.CctvCsvDownloadProperties;
import com.kbait.anchack.place.cctv.exception.CctvCsvDownloadException;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.time.Duration;
import java.util.Locale;
import java.util.Objects;

public final class CctvCsvDownloadClient {

    private static final int SUCCESS_STATUS = 200;
    private static final Charset CSV_CHARSET = Charset.forName("MS949");

    private final HttpClient httpClient;
    private final CctvCsvDownloadProperties properties;

    public CctvCsvDownloadClient(
            HttpClient httpClient,
            CctvCsvDownloadProperties properties
    ) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null.");
        this.properties = Objects.requireNonNull(properties, "properties must not be null.");
    }

    public Reader download() {
        validateDownloadSession();

        HttpResponse<InputStream> response = sendDownloadRequest();
        validateCsvResponse(response);

        return new InputStreamReader(response.body(), CSV_CHARSET);
    }

    private void validateDownloadSession() {
        HttpResponse<Void> response;
        try {
            response = httpClient.send(
                    createRequest(properties.getValidationUri()),
                    HttpResponse.BodyHandlers.discarding()
            );
        } catch (IOException exception) {
            throw new CctvCsvDownloadException("Failed to validate CCTV CSV download session.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CctvCsvDownloadException("CCTV CSV download session validation was interrupted.", exception);
        }

        if (response.statusCode() != SUCCESS_STATUS) {
            throw new CctvCsvDownloadException(
                    "CCTV CSV download session validation failed: httpStatus=" + response.statusCode()
            );
        }
    }

    private HttpResponse<InputStream> sendDownloadRequest() {
        try {
            return httpClient.send(
                    createRequest(properties.getDownloadUri()),
                    HttpResponse.BodyHandlers.ofInputStream()
            );
        } catch (IOException exception) {
            throw new CctvCsvDownloadException("Failed to download CCTV CSV.", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new CctvCsvDownloadException("CCTV CSV download was interrupted.", exception);
        }
    }

    private HttpRequest createRequest(URI uri) {
        return HttpRequest.newBuilder(uri)
                .timeout(Duration.ofMillis(properties.getRequestTimeoutMs()))
                .header("Referer", properties.getReferer())
                .header("User-Agent", properties.getUserAgent())
                .GET()
                .build();
    }

    private void validateCsvResponse(HttpResponse<InputStream> response) {
        if (response.statusCode() != SUCCESS_STATUS) {
            closeResponseBody(response.body());
            throw new CctvCsvDownloadException(
                    "CCTV CSV download failed: httpStatus=" + response.statusCode()
            );
        }

        String contentType = response.headers()
                .firstValue("Content-Type")
                .orElse("");
        if (!contentType.toLowerCase(Locale.ROOT).startsWith("text/csv")) {
            closeResponseBody(response.body());
            throw new CctvCsvDownloadException("CCTV CSV download returned an unexpected content type.");
        }
    }

    private void closeResponseBody(InputStream responseBody) {
        try {
            responseBody.close();
        } catch (IOException ignored) {
        }
    }
}
