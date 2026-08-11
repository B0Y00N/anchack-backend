package com.kbait.anchack.common.exception;

import com.kbait.anchack.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 컨트롤러/서비스에서 발생하는 예외를 그대로 흘려보내면 스택트레이스가 담긴 기본
 * 에러 페이지가 프론트로 노출된다. 클라이언트에는 ApiResponse 형태의 일관된 JSON
 * 에러 응답만 내려주고, 상세 원인은 서버 로그에만 남긴다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationFailure(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(this::toFieldMessage)
                .collect(Collectors.joining(", "));

        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.COMMON_INVALID_REQUEST, message);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(IllegalArgumentException e) {
        return buildResponse(HttpStatus.BAD_REQUEST, ErrorCode.COMMON_INVALID_REQUEST, e.getMessage());
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(RuntimeException e) {
        // 상세 원인은 서버 로그로만 확인
        e.printStackTrace();
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, ErrorCode.COMMON_INTERNAL_ERROR, null);
    }

    private String toFieldMessage(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(HttpStatus status, ErrorCode errorCode, String detail) {
        String message = (detail == null || detail.isBlank()) ? errorCode.getMessage() : detail;

        return ResponseEntity.status(status).body(ApiResponse.error(errorCode.name(), message));
    }
}
