package com.kbait.anchack.place.cctv.client;

import com.kbait.anchack.place.cctv.config.CctvCsvDownloadProperties;
import com.kbait.anchack.place.cctv.exception.CctvCsvDownloadException;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Reader;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.nio.charset.Charset;
import java.util.concurrent.atomic.AtomicBoolean;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CctvCsvDownloadClientTest {

    private static final Charset CSV_CHARSET = Charset.forName("MS949");
    private static final String USER_AGENT = "Anchack-CCTV-Test/1.0";

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void downloadKeepsSessionCookieAndDecodesMs949Csv() throws IOException {
        AtomicBoolean downloadRequestValidated = new AtomicBoolean(false);
        startServer(200, 200, "text/csv;charset=UTF-8", downloadRequestValidated);
        CctvCsvDownloadClient client = createClient();

        try (Reader reader = client.download()) {
            String actual = readAll(reader);

            assertThat(actual).startsWith("개방자치단체코드,관리번호");
            assertThat(actual).contains("202630000000800463");
        }
        assertThat(downloadRequestValidated).isTrue();
    }

    @Test
    void downloadFailsWhenSessionValidationReturnsNonSuccess() throws IOException {
        startServer(500, 200, "text/csv;charset=UTF-8", new AtomicBoolean());
        CctvCsvDownloadClient client = createClient();

        assertThatThrownBy(client::download)
                .isInstanceOf(CctvCsvDownloadException.class)
                .hasMessageContaining("httpStatus=500");
    }

    @Test
    void downloadFailsWhenResponseIsNotCsv() throws IOException {
        startServer(200, 200, "text/html", new AtomicBoolean());
        CctvCsvDownloadClient client = createClient();

        assertThatThrownBy(client::download)
                .isInstanceOf(CctvCsvDownloadException.class)
                .hasMessageContaining("unexpected content type");
    }

    private void startServer(
            int validationStatus,
            int downloadStatus,
            String contentType,
            AtomicBoolean downloadRequestValidated
    ) throws IOException {
        server = HttpServer.create(new InetSocketAddress(0), 0);
        server.createContext("/validate", exchange -> {
            assertRequestHeaders(exchange);
            exchange.getResponseHeaders().add("Set-Cookie", "CCTV_SESSION=verified; Path=/");
            exchange.sendResponseHeaders(validationStatus, -1);
            exchange.close();
        });
        server.createContext("/download", exchange -> {
            boolean hasSessionCookie = exchange.getRequestHeaders()
                    .getOrDefault("Cookie", java.util.List.of())
                    .stream()
                    .anyMatch(value -> value.contains("CCTV_SESSION=verified"));
            downloadRequestValidated.set(hasSessionCookie && hasExpectedRequestHeaders(exchange));

            byte[] body = csv().getBytes(CSV_CHARSET);
            exchange.getResponseHeaders().add("Content-Type", contentType);
            exchange.sendResponseHeaders(downloadStatus, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
    }

    private CctvCsvDownloadClient createClient() {
        String baseUrl = "http://localhost:" + server.getAddress().getPort();
        CctvCsvDownloadProperties properties = new CctvCsvDownloadProperties(
                baseUrl + "/validate",
                baseUrl + "/download",
                baseUrl + "/info",
                USER_AGENT,
                1_000,
                1_000
        );
        HttpClient httpClient = HttpClient.newBuilder()
                .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ORIGINAL_SERVER))
                .build();

        return new CctvCsvDownloadClient(httpClient, properties);
    }

    private void assertRequestHeaders(HttpExchange exchange) {
        assertThat(hasExpectedRequestHeaders(exchange)).isTrue();
    }

    private boolean hasExpectedRequestHeaders(HttpExchange exchange) {
        return exchange.getRequestMethod().equals("GET")
                && exchange.getRequestHeaders().getFirst("Referer").endsWith("/info")
                && exchange.getRequestHeaders().getFirst("User-Agent").equals(USER_AGENT);
    }

    private String readAll(Reader reader) throws IOException {
        StringBuilder result = new StringBuilder();
        char[] buffer = new char[256];
        int readCount;
        while ((readCount = reader.read(buffer)) != -1) {
            result.append(buffer, 0, readCount);
        }

        return result.toString();
    }

    private String csv() {
        return """
                개방자치단체코드,관리번호,관리기관명,소재지도로명주소,소재지지번주소,설치목적구분,카메라대수,카메라화소수,촬영방면정보,보관일수,설치연월,관리기관전화번호,WGS84위도,WGS84경도,데이터기준일자,데이터갱신구분,데이터갱신시점,최종수정시점
                3000000,202630000000800463,서울특별시 종로구청,서울특별시 종로구 북촌로 134-1,,생활방범,2,200,360도 전방면,30,202005,02-2148-3033,37.58785,126.9843,2026-05-18,,2026-05-19 22:58:23,2026-05-18 15:24:18
                """;
    }
}
