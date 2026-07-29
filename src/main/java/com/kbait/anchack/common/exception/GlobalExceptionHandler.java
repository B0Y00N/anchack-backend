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
