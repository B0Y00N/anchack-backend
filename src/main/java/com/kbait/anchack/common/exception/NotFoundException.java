package com.kbait.anchack.common.exception;

/**
 * 요청한 리소스를 찾을 수 없을 때 던진다.
 * GlobalExceptionHandler에서 HTTP 404로 변환된다.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
