package com.kbait.anchack.auth.dto.response;

/**
 * 카카오 로그인 성공 응답(POST /api/auth/kakao/callback) DTO.
 */
public class AuthLoginResponse {

    private static final String TOKEN_TYPE = "Bearer";

    private final AuthUserResponse user;
    private final String accessToken;
    private final String tokenType;

    public AuthLoginResponse(AuthUserResponse user, String accessToken) {
        this.user = user;
        this.accessToken = accessToken;
        this.tokenType = TOKEN_TYPE;
    }

    public AuthUserResponse getUser() {
        return user;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }
}
