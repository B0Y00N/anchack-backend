package com.kbait.anchack.place.cctv.exception;

public class CctvCsvDownloadException extends RuntimeException {

    public CctvCsvDownloadException(String message) {
        super(message);
    }

    public CctvCsvDownloadException(String message, Throwable cause) {
        super(message, cause);
    }
}
