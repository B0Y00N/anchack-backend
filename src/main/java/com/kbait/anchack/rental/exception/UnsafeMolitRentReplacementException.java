package com.kbait.anchack.rental.exception;

import java.time.YearMonth;

public class UnsafeMolitRentReplacementException extends RuntimeException {

    public UnsafeMolitRentReplacementException(String guCode, YearMonth dealYearMonth) {
        super(
                "기존 거래가 있는 API 유형의 신규 수집 결과가 0건이어서 "
                        + "전체 교체를 중단합니다."
                        + " guCode=" + guCode
                        + ", dealYearMonth=" + dealYearMonth
        );
    }
}
