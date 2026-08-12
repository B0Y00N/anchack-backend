package com.kbait.anchack.auth.dto.request;

/**
 * 카카오 로그인 콜백(POST /api/auth/kakao/callback) 요청 DTO.
 */
public class KakaoLoginRequest {

    private String code;

    public KakaoLoginRequest() {
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }
}
