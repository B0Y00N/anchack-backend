package com.kbait.anchack.place.cctv.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public final class PublicCctvRow {

    private final String openLocalGovernmentCode;
    private final String managementNumber;
    private final String managementAgencyName;
    private final String roadAddress;
    private final String lotAddress;
    private final String purposeType;
    private final String cameraCount;
    private final String cameraPixel;
    private final String directionInfo;
    private final String retentionDays;
    private final String installedYearMonth;
    private final String agencyPhoneNumber;
    private final String latitude;
    private final String longitude;
    private final String dataBaseDate;
    private final String dataUpdateType;
    private final String dataUpdateTime;
    private final String lastModifiedTime;
}
