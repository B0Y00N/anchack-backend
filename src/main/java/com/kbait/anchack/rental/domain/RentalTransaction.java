package com.kbait.anchack.rental.domain;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 정규화가 완료된 DB 적재용 임대차 거래 모델이다.
 *
 * <p>{@code deposit}과 {@code rent}는 원 단위로 환산하지 않고 국토부 API의 {@code 10,000 KRW}
 * 단위 그대로 저장한다.</p>
 */
@Getter
@Builder
public final class RentalTransaction {

    private final Long adminDongId;
    private final String guCode;
    private final String legalDongName;
    private final LocalDate transactionDate;
    private final String houseType;
    private final BigDecimal area;
    private final long deposit;
    private final long rent;
    private final int maintenanceFee;
}
