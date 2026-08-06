package com.kbait.anchack.ingestion.exception;

public class MolitRentApiException extends RuntimeException {

    public MolitRentApiException(String message) {
        super(message);
    }

    public MolitRentApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
