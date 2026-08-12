package com.kbait.anchack.common.exception;

import com.kbait.anchack.common.response.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * 모든 Controller와 Service에서 발생하는 예외를 공통 JSON 응답으로 변환한다.
 * 클라이언트에는 ApiResponse 형태의 일관된 응답을 제공하고,
 * 상세 예외 정보는 서버 로그에만 기록한다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log =
        LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /**
     * 요청 DTO 검증 실패
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationFailure(
        MethodArgumentNotValidException e
    ) {
        String message = e.getBindingResult()
            .getFieldErrors()
            .stream()
            .map(this::toFieldMessage)
            .collect(Collectors.joining(", "));

        return buildResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.COMMON_INVALID_REQUEST,
            message
        );
    }

    /**
     * 잘못된 요청 값
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleBadRequest(
        IllegalArgumentException e
    ) {
        return buildResponse(
            HttpStatus.BAD_REQUEST,
            ErrorCode.COMMON_INVALID_REQUEST,
            e.getMessage()
        );
    }

    /**
     * 인증되지 않은 사용자
     */
    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ApiResponse<Void>> handleUnauthorized(
        UnauthorizedException e
    ) {
        return buildResponse(
            HttpStatus.UNAUTHORIZED,
            ErrorCode.AUTH_UNAUTHORIZED,
            e.getMessage()
        );
    }

    /**
     * 접근 권한이 없는 사용자
     */
    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ApiResponse<Void>> handleForbidden(
        ForbiddenException e
    ) {
        return buildResponse(
            HttpStatus.FORBIDDEN,
            ErrorCode.AUTH_FORBIDDEN,
            e.getMessage()
        );
    }

    /**
     * 요청한 리소스를 찾을 수 없음
     */
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNotFound(
        NotFoundException e
    ) {
        return buildResponse(
            HttpStatus.NOT_FOUND,
            ErrorCode.COMMON_NOT_FOUND,
            e.getMessage()
        );
    }

    /**
     * 기존 코드에서 발생하는 SecurityException 처리
     */
    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ApiResponse<Void>> handleSecurityException(
        SecurityException e
    ) {
        String message = e.getMessage();

        boolean isAuthRequired =
            message != null && message.contains("로그인이 필요");

        HttpStatus status = isAuthRequired
            ? HttpStatus.UNAUTHORIZED
            : HttpStatus.FORBIDDEN;

        ErrorCode errorCode = isAuthRequired
            ? ErrorCode.AUTH_UNAUTHORIZED
            : ErrorCode.AUTH_FORBIDDEN;

        return buildResponse(status, errorCode, message);
    }

    /**
     * 처리되지 않은 서버 예외
     */
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse<Void>> handleRuntimeException(
        RuntimeException e
    ) {
        log.error("처리되지 않은 예외가 발생했습니다.", e);

        return buildResponse(
            HttpStatus.INTERNAL_SERVER_ERROR,
            ErrorCode.COMMON_INTERNAL_ERROR,
            null
        );
    }

    private String toFieldMessage(FieldError fieldError) {
        return fieldError.getField()
            + ": "
            + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiResponse<Void>> buildResponse(
        HttpStatus status,
        ErrorCode errorCode,
        String detail
    ) {
        String message = detail == null || detail.isBlank()
            ? errorCode.getMessage()
            : detail;

        return ResponseEntity
            .status(status)
            .body(ApiResponse.error(errorCode.name(), message));
    }
}
