package com.kbait.anchack.ingestion.exception;

public class InvalidMolitRentDataException extends RuntimeException {

    public InvalidMolitRentDataException(String message) {
        super(message);
    }

    public InvalidMolitRentDataException(String message, Throwable cause) {
        super(message, cause);
    }
}
