package com.kbait.anchack.place.exception;

public class KakaoPlaceApiException extends RuntimeException {

    public KakaoPlaceApiException(String message) {
        super(message);
    }

    public KakaoPlaceApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
