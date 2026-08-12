package com.kbait.anchack.common.exception;

/**
 * 인증되지 않은 요청에 대해 던진다.
 * GlobalExceptionHandler에서 HTTP 401로 변환된다.
 */
public class UnauthorizedException extends RuntimeException {

    public UnauthorizedException(String message) {
        super(message);
    }
}
