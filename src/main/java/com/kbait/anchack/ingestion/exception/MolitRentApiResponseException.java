package com.kbait.anchack.ingestion.exception;

import lombok.Getter;

@Getter
public class MolitRentApiResponseException extends MolitRentApiException {

    private final String resultCode;
    private final String resultMessage;

    public MolitRentApiResponseException(String resultCode, String resultMessage) {
        super(createMessage(resultCode, resultMessage));
        this.resultCode = resultCode;
        this.resultMessage = resultMessage;
    }

    private static String createMessage(String resultCode, String resultMessage) {
        return "국토부 API 응답 오류: resultCode=" + resultCode + ", resultMessage=" + resultMessage;
    }
}
