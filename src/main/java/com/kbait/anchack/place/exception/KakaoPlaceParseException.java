package com.kbait.anchack.place.exception;

public class KakaoPlaceParseException extends RuntimeException {

    public KakaoPlaceParseException(String message) {
        super(message);
    }

    public KakaoPlaceParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
