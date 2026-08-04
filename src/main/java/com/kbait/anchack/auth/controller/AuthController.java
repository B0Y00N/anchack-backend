package com.kbait.anchack.auth.controller;

import com.kbait.anchack.auth.dto.KakaoUserInfo;
import com.kbait.anchack.auth.service.KakaoAuthService;
import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.common.security.JwtTokenProvider;
import com.kbait.anchack.user.domain.User;
import com.kbait.anchack.user.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final KakaoAuthService kakaoAuthService;
    private final UserService userService;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthController(
            KakaoAuthService kakaoAuthService,
            UserService userService,
            JwtTokenProvider jwtTokenProvider
    ) {
        this.kakaoAuthService = kakaoAuthService;
        this.userService = userService;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * 카카오 로그인 콜백 처리
     *
     * 1. 카카오 인가 코드 검증
     * 2. 카카오 Access Token 발급
     * 3. 카카오 사용자 정보 조회
     * 4. users 테이블 사용자 저장 또는 갱신
     * 5. 서비스 JWT Access Token 발급
     */
    @PostMapping("/kakao/callback")
    public ResponseEntity<?> kakaoLogin(
            @RequestBody(required = false) CodeRequest request
    ) {
        // [수정] 컨트롤러 진입 여부 확인
        System.out.println();
        System.out.println("========================================");
        System.out.println("[AuthController] 카카오 로그인 요청 시작");
        System.out.println("========================================");

        if (request == null
                || request.getCode() == null
                || request.getCode().trim().isEmpty()) {

            System.err.println(
                    "[AuthController] 카카오 인가 코드가 없습니다."
            );

            return ResponseEntity
                    .badRequest()
                    .body(createErrorResponse(
                            "INVALID_CODE",
                            "카카오 인가 코드가 없습니다."
                    ));
        }

        try {
            String code = request.getCode().trim();

            // [수정] 인가 코드 원문은 출력하지 않고 존재 여부와 길이만 출력
            System.out.println(
                    "[1단계] 카카오 인가 코드 확인 완료"
            );
            System.out.println(
                    "[1단계] 인가 코드 길이: " + code.length()
            );

            /*
             * 2단계: 카카오 Access Token 발급
             */
            System.out.println(
                    "[2단계] 카카오 Access Token 발급 요청 시작"
            );

            String kakaoAccessToken =
                    kakaoAuthService.getAccessToken(code);

            if (kakaoAccessToken == null
                    || kakaoAccessToken.trim().isEmpty()) {

                throw new IllegalStateException(
                        "카카오 Access Token을 발급받지 못했습니다."
                );
            }

            System.out.println(
                    "[2단계] 카카오 Access Token 발급 완료"
            );

            /*
             * 3단계: 카카오 사용자 정보 조회
             */
            System.out.println(
                    "[3단계] 카카오 사용자 정보 조회 시작"
            );

            KakaoUserInfo kakaoUserInfo =
                    kakaoAuthService.getUserInfo(kakaoAccessToken);

            if (kakaoUserInfo == null) {
                throw new IllegalStateException(
                        "카카오 사용자 정보 응답이 null입니다."
                );
            }

            if (kakaoUserInfo.getId() == null) {
                throw new IllegalStateException(
                        "카카오 사용자 ID가 없습니다."
                );
            }

            System.out.println(
                    "[3단계] 카카오 사용자 정보 조회 완료"
            );
            System.out.println(
                    "[3단계] 카카오 사용자 ID: "
                            + kakaoUserInfo.getId()
            );

            /*
             * 4단계: 사용자 저장 또는 갱신
             */
            System.out.println(
                    "[4단계] users 테이블 사용자 저장 또는 갱신 시작"
            );

            User user =
                    userService.saveOrUpdate(kakaoUserInfo);

            if (user == null) {
                throw new IllegalStateException(
                        "UserService 결과가 null입니다."
                );
            }

            if (user.getId() == null) {
                throw new IllegalStateException(
                        "저장 또는 조회된 사용자의 user_id가 null입니다."
                );
            }

            System.out.println(
                    "[4단계] 사용자 저장 또는 갱신 완료"
            );
            System.out.println(
                    "[4단계] 서비스 사용자 ID: " + user.getId()
            );

            /*
             * 5단계: JWT Access Token 발급
             */
            System.out.println(
                    "[5단계] JWT Access Token 발급 시작"
            );

            String accessToken =
                    jwtTokenProvider.generateToken(user);

            if (accessToken == null
                    || accessToken.trim().isEmpty()) {

                throw new IllegalStateException(
                        "JWT Access Token 발급 결과가 비어 있습니다."
                );
            }

            System.out.println(
                    "[5단계] JWT Access Token 발급 완료"
            );

            Map<String, Object> responseBody =
                    new LinkedHashMap<>();

            responseBody.put("user", user);
            responseBody.put("accessToken", accessToken);
            responseBody.put("tokenType", "Bearer");

            System.out.println(
                    "[AuthController] 카카오 로그인 처리 성공"
            );
            System.out.println(
                    "========================================"
            );

            return ResponseEntity.ok(responseBody);

        } catch (HttpStatusCodeException e) {
            // [수정] 카카오 API 오류도 전체 스택 트레이스 출력
            System.err.println();
            System.err.println(
                    "[AuthController] 카카오 API 요청 실패"
            );
            System.err.println(
                    "HTTP 상태 코드: "
                            + e.getStatusCode().value()
            );
            System.err.println(
                    "카카오 응답 본문: "
                            + e.getResponseBodyAsString()
            );

            e.printStackTrace();

            Map<String, Object> responseBody =
                    createErrorResponse(
                            "KAKAO_API_ERROR",
                            "카카오 API 요청에 실패했습니다."
                    );

            responseBody.put(
                    "kakaoStatus",
                    e.getStatusCode().value()
            );

            responseBody.put(
                    "kakaoResponse",
                    e.getResponseBodyAsString()
            );

            // [수정] 개발 중 원인 확인용
            responseBody.put(
                    "exception",
                    e.getClass().getName()
            );

            return ResponseEntity
                    .status(HttpStatus.BAD_GATEWAY)
                    .body(responseBody);

        } catch (IllegalArgumentException e) {
            // [수정] IllegalArgumentException도 로그 출력
            System.err.println();
            System.err.println(
                    "[AuthController] 잘못된 로그인 요청"
            );
            System.err.println(
                    "예외 타입: " + e.getClass().getName()
            );
            System.err.println(
                    "예외 메시지: " + getExceptionMessage(e)
            );

            e.printStackTrace();

            Map<String, Object> responseBody =
                    createErrorResponse(
                            "INVALID_LOGIN_REQUEST",
                            getExceptionMessage(e)
                    );

            // [수정] 개발 중 원인 확인용
            responseBody.put(
                    "exception",
                    e.getClass().getName()
            );

            return ResponseEntity
                    .badRequest()
                    .body(responseBody);

        } catch (Exception e) {
            // [수정] 어떤 단계에서 어떤 예외가 발생했는지 명확히 출력
            System.err.println();
            System.err.println(
                    "========================================"
            );
            System.err.println(
                    "[AuthController] 로그인 처리 중 예외 발생"
            );
            System.err.println(
                    "예외 타입: " + e.getClass().getName()
            );
            System.err.println(
                    "예외 메시지: " + getExceptionMessage(e)
            );
            System.err.println(
                    "========================================"
            );

            e.printStackTrace();

            Map<String, Object> responseBody =
                    createErrorResponse(
                            "LOGIN_PROCESS_ERROR",
                            "로그인 처리 중 오류가 발생했습니다."
                    );

            /*
             * [수정]
             * 현재는 로컬 개발 중이므로 프론트에서 원인을 확인할 수 있게
             * 예외 타입과 메시지를 응답에 포함한다.
             *
             * 실제 운영 배포 시에는 exception, detail 필드를 제거해야 한다.
             */
            responseBody.put(
                    "exception",
                    e.getClass().getName()
            );

            responseBody.put(
                    "detail",
                    getExceptionMessage(e)
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBody);
        }
    }

    /**
     * JWT 인증된 현재 사용자 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> me(
            HttpServletRequest request
    ) {
        try {
            Object userIdAttribute =
                    request.getAttribute(
                            JwtAuthenticationFilter.USER_ID_ATTRIBUTE
                    );

            if (userIdAttribute == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(
                                "UNAUTHORIZED",
                                "인증 정보가 없습니다."
                        ));
            }

            Long userId =
                    convertToUserId(userIdAttribute);

            if (userId == null) {
                return ResponseEntity
                        .status(HttpStatus.UNAUTHORIZED)
                        .body(createErrorResponse(
                                "INVALID_AUTH_USER",
                                "유효하지 않은 인증 정보입니다."
                        ));
            }

            System.out.println(
                    "[AuthController] 현재 사용자 조회 ID: "
                            + userId
            );

            User user =
                    userService.findById(userId);

            if (user == null) {
                return ResponseEntity
                        .status(HttpStatus.NOT_FOUND)
                        .body(createErrorResponse(
                                "USER_NOT_FOUND",
                                "사용자 정보를 찾을 수 없습니다."
                        ));
            }

            return ResponseEntity.ok(user);

        } catch (Exception e) {
            // [수정] /me 요청 중 DB 또는 MyBatis 오류 확인
            System.err.println(
                    "[AuthController] 현재 사용자 조회 실패"
            );
            System.err.println(
                    "예외 타입: " + e.getClass().getName()
            );
            System.err.println(
                    "예외 메시지: " + getExceptionMessage(e)
            );

            e.printStackTrace();

            Map<String, Object> responseBody =
                    createErrorResponse(
                            "USER_QUERY_ERROR",
                            "사용자 조회 중 오류가 발생했습니다."
                    );

            // [수정] 로컬 개발 확인용
            responseBody.put(
                    "exception",
                    e.getClass().getName()
            );

            responseBody.put(
                    "detail",
                    getExceptionMessage(e)
            );

            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(responseBody);
        }
    }

    /**
     * JWT 로그아웃
     *
     * 서버에서 별도로 Access Token을 저장하지 않는 구조라면
     * 클라이언트 localStorage의 Access Token을 삭제하면 된다.
     */
    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        System.out.println(
                "[AuthController] 로그아웃 요청 처리"
        );

        return ResponseEntity
                .noContent()
                .build();
    }

    private Long convertToUserId(
            Object userIdAttribute
    ) {
        if (userIdAttribute instanceof Long) {
            return (Long) userIdAttribute;
        }

        if (userIdAttribute instanceof Number) {
            return ((Number) userIdAttribute).longValue();
        }

        if (userIdAttribute instanceof String) {
            try {
                return Long.valueOf(
                        ((String) userIdAttribute).trim()
                );
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    private Map<String, Object> createErrorResponse(
            String code,
            String message
    ) {
        Map<String, Object> response =
                new LinkedHashMap<>();

        response.put("code", code);
        response.put("message", message);

        return response;
    }

    /**
     * [수정]
     * 예외 메시지가 null 또는 빈 문자열일 때
     * 예외 클래스의 이름을 대신 반환한다.
     */
    private String getExceptionMessage(
            Exception e
    ) {
        if (e.getMessage() == null
                || e.getMessage().trim().isEmpty()) {

            return e.getClass().getSimpleName();
        }

        return e.getMessage();
    }

    public static class CodeRequest {

        private String code;

        public CodeRequest() {
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }
    }
}