package com.kbait.anchack.route.exception;

public class KakaoRouteApiException extends RuntimeException {

    public KakaoRouteApiException(String message) {
        super(message);
    }

    public KakaoRouteApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
