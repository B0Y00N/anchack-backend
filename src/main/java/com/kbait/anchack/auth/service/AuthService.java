package com.kbait.anchack.auth.service;

import com.kbait.anchack.auth.dto.response.AuthLoginResponse;
import com.kbait.anchack.auth.dto.response.AuthUserResponse;

/**
 * 카카오 로그인, 로그인된 사용자 조회를 담당하는 인증 서비스.
 */
public interface AuthService {

    /**
     * 카카오 인가 코드로 로그인을 처리한다.
     *
     * 1. 카카오 Access Token 발급
     * 2. 카카오 사용자 정보 조회
     * 3. users 테이블에 사용자 저장 또는 갱신
     * 4. 서비스 JWT Access Token 발급
     */
    AuthLoginResponse login(String code);

    /**
     * JWT로 인증된 사용자 정보를 조회한다. 사용자가 없으면 예외를 던진다.
     */
    AuthUserResponse getCurrentUser(Long userId);
}
