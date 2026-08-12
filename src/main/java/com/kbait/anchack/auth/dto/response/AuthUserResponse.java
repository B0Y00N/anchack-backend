package com.kbait.anchack.auth.dto.response;

import com.kbait.anchack.auth.domain.AuthUser;

/**
 * 로그인 응답과 "내 정보 조회"(GET /api/auth/me)에서 사용하는
 * 인증 사용자 응답 DTO. AuthUser 도메인 객체를 그대로 응답하지 않기 위해 사용한다.
 */
public class AuthUserResponse {

    private Long id;
    private String provider;
    private String nickname;
    private String profileImage;
    private String email;

    public AuthUserResponse() {
    }

    public static AuthUserResponse from(AuthUser authUser) {
        AuthUserResponse response = new AuthUserResponse();

        response.id = authUser.getId();
        response.provider = authUser.getProvider();
        response.nickname = authUser.getNickname();
        response.profileImage = authUser.getProfileImage();
        response.email = authUser.getEmail();

        return response;
    }

    public Long getId() {
        return id;
    }

    public String getProvider() {
        return provider;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public String getEmail() {
        return email;
    }
}
