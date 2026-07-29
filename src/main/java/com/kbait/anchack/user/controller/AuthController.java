package com.kbait.anchack.user.controller;

import com.kbait.anchack.user.dto.KakaoUserInfo;
import com.kbait.anchack.user.model.KakaoMember;
import com.kbait.anchack.user.service.KakaoAuthService;
import com.kbait.anchack.user.service.MemberService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpSession;

/**
 * 카카오 로그인 API
 * - 프론트(Vue)에서 카카오 인가 코드를 받아 access_token 교환 -> 사용자 정보 조회
 * - DB(MySQL)에 회원 저장(최초 로그인) / 갱신(기존 회원) 후 세션에 저장
 */
@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class AuthController {

    @Autowired
    private KakaoAuthService kakaoAuthService;

    @Autowired
    private MemberService memberService;

    @PostMapping("/kakao/callback")
    public KakaoMember kakaoLogin(@RequestBody CodeRequest request, HttpSession session) {
        String accessToken = kakaoAuthService.getAccessToken(request.getCode());
        KakaoUserInfo userInfo = kakaoAuthService.getUserInfo(accessToken);

        // DB에 회원 저장(최초) 또는 갱신(기존)
        KakaoMember member = memberService.saveOrUpdate(userInfo);

        // 간단 예제이므로 세션에 로그인 회원 정보 저장
        // (실무에서는 세션 대신 JWT 발급 방식도 고려)
        session.setAttribute("LOGIN_USER", member);

        return member;
    }

    @GetMapping("/me")
    public ResponseEntity<KakaoMember> me(HttpSession session) {
        KakaoMember member = (KakaoMember) session.getAttribute("LOGIN_USER");

        if (member == null) {
            return ResponseEntity.noContent().build();
        }

        return ResponseEntity.ok(member);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpSession session) {
        session.invalidate();
        return ResponseEntity.noContent().build();
    }

    public static class CodeRequest {
        private String code;

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}
