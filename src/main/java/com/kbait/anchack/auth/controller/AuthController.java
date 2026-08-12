package com.kbait.anchack.auth.controller;

import com.kbait.anchack.auth.dto.request.KakaoLoginRequest;
import com.kbait.anchack.auth.dto.response.AuthLoginResponse;
import com.kbait.anchack.auth.dto.response.AuthUserResponse;
import com.kbait.anchack.auth.service.AuthService;
import com.kbait.anchack.common.security.AuthenticatedUserResolver;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;

/**
 * 카카오 로그인, 로그인 사용자 조회, 로그아웃을 처리한다.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /**
     * 카카오 로그인 콜백. 프론트가 카카오에서 받은 인가 코드를 전달하면
     * 서비스 JWT Access Token을 발급해서 응답한다.
     */
    @PostMapping("/kakao/callback")
    public ResponseEntity<AuthLoginResponse> kakaoLogin(
        @RequestBody(required = false) KakaoLoginRequest request
    ) {
        String code = request == null ? null : request.getCode();

        AuthLoginResponse response = authService.login(code);

        return ResponseEntity.ok(response);
    }

    /**
     * JWT로 인증된 현재 로그인 사용자 정보를 조회한다.
     */
    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> me(HttpServletRequest request) {
        Long userId = AuthenticatedUserResolver.requireUserId(request);

        return ResponseEntity.ok(authService.getCurrentUser(userId));
    }

    /**
     * 로그아웃. 서버에서 Access Token을 별도로 저장하지 않으므로
     * 클라이언트가 저장된 토큰을 삭제하도록 204만 응답한다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.noContent().build();
    }
}
