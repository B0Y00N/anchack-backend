package com.kbait.anchack.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * [신규] 컨트롤러/서비스에서 발생하는 예외를 그대로 흘려보내면
 * 스택트레이스가 담긴 기본 500 에러 페이지가 프론트로 그대로 노출된다.
 * 클라이언트에는 일관된 JSON 에러 응답만 내려주고, 상세 원인은 서버 로그에만 남긴다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, e.getMessage());
    }

    /*
     * [수정] SecurityException(로그인 필요/권한 없음 등)이 별도 핸들러 없이
     * 아래 RuntimeException 핸들러로 흘러가 502 Bad Gateway로 응답되고 있었다.
     * ReviewController.getAuthenticatedUserId(), ReviewService.validateOwner(),
     * ReviewService.validateAdmin() 등이 던지는 SecurityException은
     * 인증(401)/인가(403) 문제이므로 그에 맞는 상태 코드로 응답해야 프론트가
     * "로그인이 필요합니다" 같은 메시지를 정확히 처리할 수 있다.
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<Map<String, Object>> handleSecurityException(SecurityException e) {
        String message = e.getMessage();
        boolean isAuthRequired = message != null && message.contains("로그인이 필요");
        HttpStatus status = isAuthRequired ? HttpStatus.UNAUTHORIZED : HttpStatus.FORBIDDEN;
        return buildResponse(status, message);
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<Map<String, Object>> handleRuntimeException(RuntimeException e) {
        // 카카오 API 연동 실패, DB 오류 등 상세 원인은 서버 로그로만 확인
        e.printStackTrace();
        return buildResponse(HttpStatus.BAD_GATEWAY, "요청 처리 중 오류가 발생했습니다.");
    }

    private ResponseEntity<Map<String, Object>> buildResponse(HttpStatus status, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("status", status.value());
        body.put("message", message);
        return ResponseEntity.status(status).body(body);
    }
}
