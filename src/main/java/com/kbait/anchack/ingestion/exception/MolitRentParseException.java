package com.kbait.anchack.ingestion.exception;

public class MolitRentParseException extends MolitRentApiException {

    public MolitRentParseException(String message) {
        super(message);
    }

    public MolitRentParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
