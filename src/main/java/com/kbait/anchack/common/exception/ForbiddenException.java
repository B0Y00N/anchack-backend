package com.kbait.anchack.common.exception;

/**
 * 인증은 되었으나 권한이 없는 요청에 대해 던진다.
 * GlobalExceptionHandler에서 HTTP 403으로 변환된다.
 */
public class ForbiddenException extends RuntimeException {

    public ForbiddenException(String message) {
        super(message);
    }
}
