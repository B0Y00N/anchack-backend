package com.kbait.anchack.admindong.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * 행정동 상세(P1-b)의 보증금/월세 중위값·구간별 분포 계산용 원본 거래 1건.
 * rental_transactions의 deposit_amount/monthly_rent_amount는 국토부 API 단위(만원) 그대로
 * 저장되어 있어 별도 환산 없이 문서가 요구하는 "만원" 단위로 바로 쓸 수 있다.
 */
@Getter
@Setter
public class RentalAmountRow {

    private Long adminDongId;
    private Long depositAmount;
    private Integer monthlyRentAmount;
}
