package com.kbait.anchack.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kbait.anchack.auth.dto.KakaoUserInfo;
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

@Service
public class KakaoAuthService {

    @Value("${kakao.client-id}")
    private String clientId;

    @Value("${kakao.client-secret:}")
    private String clientSecret;

    @Value("${kakao.redirect-uri}")
    private String redirectUri;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 카카오 인가 코드로 Access Token 발급
     */
    public String getAccessToken(String code) {

        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException(
                    "카카오 인가 코드가 비어 있습니다."
            );
        }

        String url = "https://kauth.kakao.com/oauth/token";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(
                MediaType.APPLICATION_FORM_URLENCODED
        );
        headers.setAccept(
                Collections.singletonList(MediaType.APPLICATION_JSON)
        );

        MultiValueMap<String, String> params =
                new LinkedMultiValueMap<>();

        params.add("grant_type", "authorization_code");
        params.add("client_id", clientId.trim());
        params.add("redirect_uri", redirectUri.trim());
        params.add("code", code.trim());

        /*
         * 카카오 개발자 콘솔에서 Client Secret 사용 설정이 ON인 경우에만
         * client_secret을 전송합니다.
         */
        if (StringUtils.hasText(clientSecret)) {
            params.add(
                    "client_secret",
                    clientSecret.trim()
            );
        }

        HttpEntity<MultiValueMap<String, String>> request =
                new HttpEntity<>(params, headers);

        try {

            ResponseEntity<String> response =
                    restTemplate.postForEntity(
                            url,
                            request,
                            String.class
                    );

            String responseBody = response.getBody();

            if (!StringUtils.hasText(responseBody)) {
                throw new RuntimeException(
                        "카카오 토큰 응답이 비어 있습니다."
                );
            }

            JsonNode json =
                    objectMapper.readTree(responseBody);

            JsonNode accessTokenNode =
                    json.get("access_token");

            if (accessTokenNode == null
                    || accessTokenNode.isNull()
                    || !StringUtils.hasText(
                    accessTokenNode.asText()
            )) {

                throw new RuntimeException(
                        "카카오 응답에 access_token이 없습니다. 응답: "
                                + responseBody
                );
            }

            return accessTokenNode.asText();

        } catch (HttpClientErrorException e) {

            String errorBody =
                    e.getResponseBodyAsString();

            System.err.println(
                    "========== 카카오 토큰 발급 실패 =========="
            );
            System.err.println(
                    "HTTP 상태: " + e.getStatusCode()
            );
            System.err.println(
                    "응답 내용: " + errorBody
            );
            System.err.println(
                    "clientId: " + maskClientId(clientId)
            );
            System.err.println(
                    "redirectUri: " + redirectUri
            );
            System.err.println(
                    "Client Secret 전송 여부: "
                            + StringUtils.hasText(clientSecret)
            );
            System.err.println(
                    "========================================"
            );

            throw new RuntimeException(
                    "카카오 토큰 발급 실패: "
                            + e.getStatusCode()
                            + " / "
                            + errorBody,
                    e
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "카카오 액세스 토큰 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * Access Token으로 카카오 사용자 정보 조회
     */
    public KakaoUserInfo getUserInfo(String accessToken) {

        if (!StringUtils.hasText(accessToken)) {
            throw new IllegalArgumentException(
                    "카카오 Access Token이 비어 있습니다."
            );
        }

        String url =
                "https://kapi.kakao.com/v2/user/me";

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setAccept(
                Collections.singletonList(MediaType.APPLICATION_JSON)
        );

        HttpEntity<Void> request =
                new HttpEntity<>(headers);

        try {

            ResponseEntity<String> response =
                    restTemplate.exchange(
                            url,
                            HttpMethod.GET,
                            request,
                            String.class
                    );

            String responseBody =
                    response.getBody();

            if (!StringUtils.hasText(responseBody)) {
                throw new RuntimeException(
                        "카카오 사용자 정보 응답이 비어 있습니다."
                );
            }

            JsonNode json =
                    objectMapper.readTree(responseBody);

            JsonNode idNode =
                    json.get("id");

            if (idNode == null || idNode.isNull()) {
                throw new RuntimeException(
                        "카카오 사용자 응답에 id가 없습니다. 응답: "
                                + responseBody
                );
            }

            Long id =
                    idNode.asLong();

            JsonNode kakaoAccount =
                    json.get("kakao_account");

            JsonNode profile =
                    null;

            if (kakaoAccount != null
                    && !kakaoAccount.isNull()) {

                profile =
                        kakaoAccount.get("profile");
            }

            String nickname =
                    null;

            String profileImage =
                    null;

            String email =
                    null;

            String birthYear =
                    null;

            String birthday =
                    null;

            LocalDate birthDate =
                    null;

            /*
             * 프로필 정보 추출
             */
            if (profile != null
                    && !profile.isNull()) {

                JsonNode nicknameNode =
                        profile.get("nickname");

                if (nicknameNode != null
                        && !nicknameNode.isNull()) {

                    nickname =
                            nicknameNode.asText();
                }

                JsonNode profileImageNode =
                        profile.get("profile_image_url");

                if (profileImageNode != null
                        && !profileImageNode.isNull()) {

                    profileImage =
                            profileImageNode.asText();
                }
            }

            /*
             * 카카오 계정 정보 추출
             */
            if (kakaoAccount != null
                    && !kakaoAccount.isNull()) {

                JsonNode emailNode =
                        kakaoAccount.get("email");

                if (emailNode != null
                        && !emailNode.isNull()) {

                    email =
                            emailNode.asText();
                }

                JsonNode birthYearNode =
                        kakaoAccount.get("birthyear");

                if (birthYearNode != null
                        && !birthYearNode.isNull()) {

                    birthYear =
                            birthYearNode.asText();
                }

                JsonNode birthdayNode =
                        kakaoAccount.get("birthday");

                if (birthdayNode != null
                        && !birthdayNode.isNull()) {

                    birthday =
                            birthdayNode.asText();
                }
            }

            /*
             * birthyear와 birthday를 결합해서 LocalDate로 변환
             *
             * 예:
             * birthyear = "1997"
             * birthday = "0315"
             *
             * 결과:
             * birthDate = 1997-03-15
             */
            birthDate =
                    convertToBirthDate(
                            birthYear,
                            birthday
                    );

            KakaoUserInfo userInfo =
                    new KakaoUserInfo();

            userInfo.setId(id);
            userInfo.setNickname(nickname);
            userInfo.setProfileImage(profileImage);
            userInfo.setEmail(email);
            userInfo.setBirthYear(birthYear);
            userInfo.setBirthday(birthday);
            userInfo.setBirthDate(birthDate);

            return userInfo;

        } catch (HttpClientErrorException e) {

            String errorBody =
                    e.getResponseBodyAsString();

            System.err.println(
                    "========== 카카오 사용자 조회 실패 =========="
            );
            System.err.println(
                    "HTTP 상태: " + e.getStatusCode()
            );
            System.err.println(
                    "응답 내용: " + errorBody
            );
            System.err.println(
                    "=========================================="
            );

            throw new RuntimeException(
                    "카카오 사용자 정보 조회 실패: "
                            + e.getStatusCode()
                            + " / "
                            + errorBody,
                    e
            );

        } catch (Exception e) {

            throw new RuntimeException(
                    "카카오 사용자 정보 처리 중 오류가 발생했습니다.",
                    e
            );
        }
    }

    /**
     * 카카오 출생연도와 생일을 LocalDate로 변환합니다.
     *
     * @param birthYear 출생연도, 예: 1997
     * @param birthday  생일, 예: 0315
     * @return 변환된 생년월일, 값이 없거나 잘못된 경우 null
     */
    private LocalDate convertToBirthDate(
            String birthYear,
            String birthday
    ) {

        if (!StringUtils.hasText(birthYear)
                || !StringUtils.hasText(birthday)) {

            return null;
        }

        String birthDateText =
                birthYear.trim()
                        + birthday.trim();

        DateTimeFormatter formatter =
                DateTimeFormatter.ofPattern("yyyyMMdd");

        try {

            return LocalDate.parse(
                    birthDateText,
                    formatter
            );

        } catch (DateTimeParseException e) {

            System.err.println(
                    "카카오 생년월일 변환 실패: "
                            + birthDateText
            );

            return null;
        }
    }

    /**
     * 로그에 REST API 키 전체가 노출되지 않도록 일부만 표시합니다.
     */
    private String maskClientId(String value) {

        if (!StringUtils.hasText(value)) {
            return "(비어 있음)";
        }

        String trimmedValue =
                value.trim();

        if (trimmedValue.length() <= 8) {
            return "********";
        }

        return trimmedValue.substring(0, 4)
                + "********"
                + trimmedValue.substring(
                trimmedValue.length() - 4
        );
    }
}