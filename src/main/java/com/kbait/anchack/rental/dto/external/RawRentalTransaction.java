package com.kbait.anchack.rental.dto.external;

import com.kbait.anchack.rental.client.MolitRentApiCategory;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class RawRentalTransaction {

    private final MolitRentApiCategory apiCategory;

    private final String guCode;
    private final String legalDongName;
    private final String jibun;

    private final String dealYear;
    private final String dealMonth;
    private final String dealDay;

    private final String houseType;
    private final String exclusiveArea;
    private final String totalFloorArea;

    private final String deposit;
    private final String monthlyRent;
}
