package com.kbait.anchack.ingestion.normalizer;

import com.kbait.anchack.ingestion.client.MolitRentApiCategory;
import com.kbait.anchack.ingestion.domain.RentalTransaction;
import com.kbait.anchack.ingestion.dto.external.RawRentalTransaction;
import com.kbait.anchack.ingestion.exception.InvalidMolitRentDataException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.Set;
import java.util.regex.Pattern;

public final class RentalTransactionNormalizer {

    private static final Pattern GU_CODE_PATTERN = Pattern.compile("[0-9]{5}");
    private static final Pattern PLAIN_AMOUNT_PATTERN = Pattern.compile("[0-9]+");
    private static final Pattern GROUPED_AMOUNT_PATTERN = Pattern.compile("[1-9][0-9]{0,2}(,[0-9]{3})+");
    private static final Pattern AREA_PATTERN = Pattern.compile("[0-9]+(\\.[0-9]+)?");

    private static final Set<String> ROW_HOUSE_TYPES = Set.of("연립", "다세대", "연립다세대");
    private static final Set<String> SINGLE_HOUSE_TYPES = Set.of("단독", "다가구");

    private static final BigDecimal MAX_AREA = new BigDecimal("999.99");

    public RentalTransaction normalize(
            RawRentalTransaction rawTransaction,
            LocalDate dataDate
    ) {
        if (rawTransaction == null) {
            throw new InvalidMolitRentDataException("rawTransaction은 null일 수 없습니다.");
        }
        if (dataDate == null) {
            throw new InvalidMolitRentDataException("dataDate는 null일 수 없습니다.");
        }

        MolitRentApiCategory apiCategory = requireApiCategory(rawTransaction.getApiCategory());

        return RentalTransaction.builder()
                .adminDongId(null)
                .guCode(normalizeGuCode(rawTransaction.getGuCode()))
                .legalDongName(normalizeLegalDongName(rawTransaction.getLegalDongName()))
                .transactionDate(normalizeTransactionDate(rawTransaction))
                .houseType(normalizeHouseType(apiCategory, rawTransaction.getHouseType()))
                .area(normalizeArea(apiCategory, rawTransaction))
                .deposit(normalizeAmount(rawTransaction.getDeposit(), "deposit"))
                .rent(normalizeAmount(rawTransaction.getMonthlyRent(), "monthlyRent"))
                .maintenanceFee(0)
                .dataDate(dataDate)
                .build();
    }

    private MolitRentApiCategory requireApiCategory(MolitRentApiCategory apiCategory) {
        if (apiCategory == null) {
            throw new InvalidMolitRentDataException("apiCategory는 null일 수 없습니다.");
        }

        return apiCategory;
    }

    private String normalizeGuCode(String guCode) {
        String normalizedGuCode = requireText(guCode, "guCode");

        if (!GU_CODE_PATTERN.matcher(normalizedGuCode).matches()) {
            throw new InvalidMolitRentDataException("guCode는 숫자 5자리여야 합니다.");
        }

        return normalizedGuCode;
    }

    private String normalizeLegalDongName(String legalDongName) {
        String normalizedLegalDongName = requireText(legalDongName, "legalDongName");
        int length = normalizedLegalDongName.codePointCount(0, normalizedLegalDongName.length());

        if (length > 100) {
            throw new InvalidMolitRentDataException("legalDongName은 100자를 초과할 수 없습니다.");
        }

        return normalizedLegalDongName;
    }

    private LocalDate normalizeTransactionDate(RawRentalTransaction rawTransaction) {
        String dealYear = requireText(rawTransaction.getDealYear(), "dealYear");
        String dealMonth = requireText(rawTransaction.getDealMonth(), "dealMonth");
        String dealDay = requireText(rawTransaction.getDealDay(), "dealDay");

        try {
            int year = Integer.parseInt(dealYear);
            int month = Integer.parseInt(dealMonth);
            int day = Integer.parseInt(dealDay);
            return LocalDate.of(year, month, day);
        } catch (NumberFormatException exception) {
            throw new InvalidMolitRentDataException("거래일 숫자 형식이 올바르지 않습니다.", exception);
        } catch (DateTimeException exception) {
            throw new InvalidMolitRentDataException("존재하지 않는 거래일입니다.", exception);
        }
    }

    private long normalizeAmount(String amount, String fieldName) {
        String normalizedAmount = requireText(amount, fieldName);

        if (!isValidAmountFormat(normalizedAmount)) {
            throw new InvalidMolitRentDataException(fieldName + " 금액 형식이 올바르지 않습니다.");
        }

        try {
            return Long.parseLong(normalizedAmount.replace(",", ""));
        } catch (NumberFormatException exception) {
            throw new InvalidMolitRentDataException(
                    fieldName + " 금액 범위가 올바르지 않습니다.",
                    exception
            );
        }
    }

    private boolean isValidAmountFormat(String amount) {
        return PLAIN_AMOUNT_PATTERN.matcher(amount).matches()
                || GROUPED_AMOUNT_PATTERN.matcher(amount).matches();
    }

    private String normalizeHouseType(MolitRentApiCategory apiCategory, String houseType) {
        if (apiCategory == MolitRentApiCategory.OFFICETEL) {
            return "오피스텔";
        }
        if (apiCategory == MolitRentApiCategory.ROW_HOUSE) {
            return requireAllowedHouseType(houseType, ROW_HOUSE_TYPES);
        }
        if (apiCategory == MolitRentApiCategory.SINGLE_HOUSE) {
            return requireAllowedHouseType(houseType, SINGLE_HOUSE_TYPES);
        }

        throw new InvalidMolitRentDataException("지원하지 않는 국토부 전월세 API 종류입니다.");
    }

    private String requireAllowedHouseType(String houseType, Set<String> allowedHouseTypes) {
        String normalizedHouseType = requireText(houseType, "houseType");

        if (!allowedHouseTypes.contains(normalizedHouseType)) {
            throw new InvalidMolitRentDataException("API 종류에 허용되지 않는 houseType입니다.");
        }

        return normalizedHouseType;
    }

    private BigDecimal normalizeArea(
            MolitRentApiCategory apiCategory,
            RawRentalTransaction rawTransaction
    ) {
        String selectedArea = selectArea(apiCategory, rawTransaction);
        String normalizedArea = requireText(selectedArea, "area");

        if (!AREA_PATTERN.matcher(normalizedArea).matches()) {
            throw new InvalidMolitRentDataException("area 숫자 형식이 올바르지 않습니다.");
        }

        try {
            return requireStorableArea(new BigDecimal(normalizedArea));
        } catch (NumberFormatException exception) {
            throw new InvalidMolitRentDataException("area 숫자 형식이 올바르지 않습니다.", exception);
        }
    }

    private String selectArea(
            MolitRentApiCategory apiCategory,
            RawRentalTransaction rawTransaction
    ) {
        if (apiCategory == MolitRentApiCategory.OFFICETEL
                || apiCategory == MolitRentApiCategory.ROW_HOUSE) {
            return rawTransaction.getExclusiveArea();
        }
        if (apiCategory == MolitRentApiCategory.SINGLE_HOUSE) {
            return rawTransaction.getTotalFloorArea();
        }

        throw new InvalidMolitRentDataException("지원하지 않는 국토부 전월세 API 종류입니다.");
    }

    private BigDecimal requireStorableArea(BigDecimal area) {
        BigDecimal roundedArea = area.setScale(2, RoundingMode.HALF_UP);

        if (roundedArea.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidMolitRentDataException("area는 0보다 커야 합니다.");
        }
        if (roundedArea.compareTo(MAX_AREA) > 0) {
            throw new InvalidMolitRentDataException("area는 999.99를 초과할 수 없습니다.");
        }

        return roundedArea;
    }

    private String requireText(String value, String fieldName) {
        if (value == null) {
            throw new InvalidMolitRentDataException(fieldName + "은 null일 수 없습니다.");
        }

        String normalizedValue = value.trim();
        if (normalizedValue.isEmpty()) {
            throw new InvalidMolitRentDataException(fieldName + "은 빈 값일 수 없습니다.");
        }

        return normalizedValue;
    }
}
