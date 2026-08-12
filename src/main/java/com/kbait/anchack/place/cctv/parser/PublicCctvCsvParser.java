package com.kbait.anchack.place.cctv.parser;

import com.kbait.anchack.place.cctv.dto.PublicCctvRow;
import com.kbait.anchack.place.cctv.exception.PublicCctvCsvParseException;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class PublicCctvCsvParser {

    private static final String OPEN_LOCAL_GOVERNMENT_CODE = "개방자치단체코드";
    private static final String MANAGEMENT_NUMBER = "관리번호";
    private static final String MANAGEMENT_AGENCY_NAME = "관리기관명";
    private static final String ROAD_ADDRESS = "소재지도로명주소";
    private static final String LOT_ADDRESS = "소재지지번주소";
    private static final String PURPOSE_TYPE = "설치목적구분";
    private static final String CAMERA_COUNT = "카메라대수";
    private static final String CAMERA_PIXEL = "카메라화소수";
    private static final String DIRECTION_INFO = "촬영방면정보";
    private static final String RETENTION_DAYS = "보관일수";
    private static final String INSTALLED_YEAR_MONTH = "설치연월";
    private static final String AGENCY_PHONE_NUMBER = "관리기관전화번호";
    private static final String LATITUDE = "WGS84위도";
    private static final String LONGITUDE = "WGS84경도";
    private static final String DATA_BASE_DATE = "데이터기준일자";
    private static final String DATA_UPDATE_TYPE = "데이터갱신구분";
    private static final String DATA_UPDATE_TIME = "데이터갱신시점";
    private static final String LAST_MODIFIED_TIME = "최종수정시점";

    private static final List<String> REQUIRED_HEADERS = List.of(
            OPEN_LOCAL_GOVERNMENT_CODE,
            MANAGEMENT_NUMBER,
            MANAGEMENT_AGENCY_NAME,
            ROAD_ADDRESS,
            LOT_ADDRESS,
            PURPOSE_TYPE,
            CAMERA_COUNT,
            CAMERA_PIXEL,
            DIRECTION_INFO,
            RETENTION_DAYS,
            INSTALLED_YEAR_MONTH,
            AGENCY_PHONE_NUMBER,
            LATITUDE,
            LONGITUDE,
            DATA_BASE_DATE,
            DATA_UPDATE_TYPE,
            DATA_UPDATE_TIME,
            LAST_MODIFIED_TIME
    );

    private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.builder()
            .setHeader()
            .setSkipHeaderRecord(true)
            .setIgnoreEmptyLines(true)
            .get();

    public List<PublicCctvRow> parse(Reader reader) {
        Objects.requireNonNull(reader, "reader must not be null.");

        try (CSVParser csvParser = CSV_FORMAT.parse(reader)) {
            validateHeaders(csvParser);

            List<PublicCctvRow> rows = new ArrayList<>();
            for (CSVRecord record : csvParser) {
                validateRecord(record);
                rows.add(toRow(record));
            }
            return rows;
        } catch (IOException | IllegalArgumentException exception) {
            throw new PublicCctvCsvParseException("Failed to parse public CCTV CSV.", exception);
        }
    }

    private void validateHeaders(CSVParser csvParser) {
        for (String requiredHeader : REQUIRED_HEADERS) {
            if (!csvParser.getHeaderMap().containsKey(requiredHeader)) {
                throw new PublicCctvCsvParseException(
                        "Required CCTV CSV header is missing: " + requiredHeader
                );
            }
        }
    }

    private void validateRecord(CSVRecord record) {
        if (!record.isConsistent()) {
            throw new PublicCctvCsvParseException(
                    "CCTV CSV record has an invalid column count at record " + record.getRecordNumber()
            );
        }
    }

    private PublicCctvRow toRow(CSVRecord record) {
        return PublicCctvRow.builder()
                .openLocalGovernmentCode(record.get(OPEN_LOCAL_GOVERNMENT_CODE))
                .managementNumber(record.get(MANAGEMENT_NUMBER))
                .managementAgencyName(record.get(MANAGEMENT_AGENCY_NAME))
                .roadAddress(record.get(ROAD_ADDRESS))
                .lotAddress(record.get(LOT_ADDRESS))
                .purposeType(record.get(PURPOSE_TYPE))
                .cameraCount(record.get(CAMERA_COUNT))
                .cameraPixel(record.get(CAMERA_PIXEL))
                .directionInfo(record.get(DIRECTION_INFO))
                .retentionDays(record.get(RETENTION_DAYS))
                .installedYearMonth(record.get(INSTALLED_YEAR_MONTH))
                .agencyPhoneNumber(record.get(AGENCY_PHONE_NUMBER))
                .latitude(record.get(LATITUDE))
                .longitude(record.get(LONGITUDE))
                .dataBaseDate(record.get(DATA_BASE_DATE))
                .dataUpdateType(record.get(DATA_UPDATE_TYPE))
                .dataUpdateTime(record.get(DATA_UPDATE_TIME))
                .lastModifiedTime(record.get(LAST_MODIFIED_TIME))
                .build();
    }
}
