package com.kbait.anchack.place.exception;

public class InvalidPlaceDataException extends RuntimeException {

    public InvalidPlaceDataException(String message) {
        super(message);
    }

    public InvalidPlaceDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
