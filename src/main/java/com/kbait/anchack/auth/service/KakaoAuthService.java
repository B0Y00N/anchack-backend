package com.kbait.anchack.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.auth.dto.KakaoUserInfo;
import com.kbait.anchack.auth.exception.KakaoAuthException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;

/**
 * 카카오 OAuth API(토큰 발급, 사용자 정보 조회)를 호출하는 외부 연동 담당 클래스.
 *
 * 카카오 응답 JSON을 그대로 쓰지 않고 KakaoUserInfo로 변환해서,
 * 카카오 API가 바뀌어도 영향 범위를 이 클래스로 한정한다.
 */
@Service
public class KakaoAuthService {

    private static final Logger log = LoggerFactory.getLogger(KakaoAuthService.class);

    private static final String TOKEN_URL = "https://kauth.kakao.com/oauth/token";
    private static final String USER_INFO_URL = "https://kapi.kakao.com/v2/user/me";

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KakaoAuthService(
        @Qualifier("restTemplate") RestTemplate restTemplate,
        ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    /**
     * 카카오 인가 코드로 Access Token을 발급받는다.
     */
    public String getAccessToken(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("카카오 인가 코드가 비어 있습니다.");
        }

        HttpEntity<MultiValueMap<String, String>> request = createTokenRequest(code.trim());

        try {
            ResponseEntity<String> response =
                restTemplate.postForEntity(TOKEN_URL, request, String.class);

            return extractAccessToken(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error(
                "카카오 토큰 발급 실패. status={}, clientId={}",
                e.getStatusCode(),
                maskClientId(clientId)
            );
            log.debug("카카오 토큰 발급 실패 응답: {}", e.getResponseBodyAsString());

            throw new KakaoAuthException("카카오 토큰 발급에 실패했습니다.", e);

        } catch (KakaoAuthException e) {
            throw e;

        } catch (Exception e) {
            throw new KakaoAuthException("카카오 액세스 토큰 처리 중 오류가 발생했습니다.", e);
        }
    }

    /**
     * Access Token으로 카카오 사용자 정보를 조회한다.
     */
    public KakaoUserInfo getUserInfo(String accessToken) {
        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException("카카오 Access Token이 비어 있습니다.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        HttpEntity<Void> request = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                restTemplate.exchange(USER_INFO_URL, HttpMethod.GET, request, String.class);

            return parseUserInfo(response.getBody());

        } catch (HttpClientErrorException e) {
            log.error("카카오 사용자 정보 조회 실패. status={}", e.getStatusCode());
            log.debug("카카오 사용자 정보 조회 실패 응답: {}", e.getResponseBodyAsString());

            throw new KakaoAuthException("카카오 사용자 정보 조회에 실패했습니다.", e);

        } catch (KakaoAuthException e) {
            throw e;

        } catch (Exception e) {
            throw new KakaoAuthException("카카오 사용자 정보 처리 중 오류가 발생했습니다.", e);
        }
    }

    private HttpEntity<MultiValueMap<String, String>> createTokenRequest(String code) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId.trim());
        params.add("redirect_uri", redirectUri.trim());
        params.add("code", code);

        // 카카오 개발자 콘솔에서 Client Secret 사용 설정이 ON인 경우에만 전송한다.
        if (StringUtils.hasText(clientSecret)) {
            params.add("client_secret", clientSecret.trim());
        }

        return new HttpEntity<>(params, headers);
    }

    private String extractAccessToken(String responseBody) throws Exception {
        if (!StringUtils.hasText(responseBody)) {
            throw new KakaoAuthException("카카오 토큰 응답이 비어 있습니다.");
        }

        JsonNode json = objectMapper.readTree(responseBody);
        JsonNode accessTokenNode = json.get("access_token");

        if (accessTokenNode == null || accessTokenNode.isNull()
            || !StringUtils.hasText(accessTokenNode.asText())) {
            throw new KakaoAuthException("카카오 응답에 access_token이 없습니다.");
        }

        return accessTokenNode.asText();
    }

    private KakaoUserInfo parseUserInfo(String responseBody) throws Exception {
        if (!StringUtils.hasText(responseBody)) {
            throw new KakaoAuthException("카카오 사용자 정보 응답이 비어 있습니다.");
        }

        JsonNode json = objectMapper.readTree(responseBody);
        JsonNode idNode = json.get("id");

        if (idNode == null || idNode.isNull()) {
            throw new KakaoAuthException("카카오 사용자 응답에 id가 없습니다.");
        }

        JsonNode kakaoAccount = json.get("kakao_account");
        JsonNode profile = (kakaoAccount != null && !kakaoAccount.isNull())
            ? kakaoAccount.get("profile")
            : null;

        KakaoUserInfo userInfo = new KakaoUserInfo();
        userInfo.setId(idNode.asLong());
        userInfo.setNickname(textOrNull(profile, "nickname"));
        userInfo.setProfileImage(textOrNull(profile, "profile_image_url"));
        userInfo.setEmail(textOrNull(kakaoAccount, "email"));

        String birthYear = textOrNull(kakaoAccount, "birthyear");
        String birthday = textOrNull(kakaoAccount, "birthday");

        userInfo.setBirthYear(birthYear);
        userInfo.setBirthday(birthday);
        userInfo.setBirthDate(convertToBirthDate(birthYear, birthday));

        return userInfo;
    }

    private String textOrNull(JsonNode parent, String fieldName) {
        if (parent == null || parent.isNull()) {
            return null;
        }

        JsonNode field = parent.get(fieldName);

        if (field == null || field.isNull()) {
            return null;
        }

        return field.asText();
    }

    /**
     * 카카오 출생연도(yyyy)와 생일(MMdd)을 하나의 LocalDate로 합친다.
     * 값이 없거나 형식이 올바르지 않으면 null을 반환한다.
     */
    private LocalDate convertToBirthDate(String birthYear, String birthday) {
        if (!StringUtils.hasText(birthYear) || !StringUtils.hasText(birthday)) {
            return null;
        }

        String birthDateText = birthYear.trim() + birthday.trim();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd");

        try {
            return LocalDate.parse(birthDateText, formatter);
        } catch (DateTimeParseException e) {
            log.warn("카카오 생년월일 변환에 실패했습니다.");
            return null;
        }
    }

    /**
     * 로그에 REST API 키 전체가 남지 않도록 일부만 남긴다.
     */
    private String maskClientId(String value) {
        if (!StringUtils.hasText(value)) {
            return "(비어 있음)";
        }

        String trimmedValue = value.trim();

        if (trimmedValue.length() <= 8) {
            return "********";
        }

        return trimmedValue.substring(0, 4)
            + "********"
            + trimmedValue.substring(trimmedValue.length() - 4);
    }
}
