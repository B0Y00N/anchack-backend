package com.kbait.anchack.rental.config;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Getter
public final class MolitRentSchedulerProperties {

    private static final String DISABLED_CRON = "-";
    private static final Pattern LAWD_CODE_PATTERN = Pattern.compile("[0-9]{5}");

    private final String cron;
    private final List<String> lawdCodes;

    public MolitRentSchedulerProperties(String cron, String lawdCodes) {
        this.cron = Objects.requireNonNull(cron, "cron은 null일 수 없습니다.");
        this.lawdCodes = parseLawdCodes(
                Objects.requireNonNull(lawdCodes, "법정 시군구 코드 설정은 null일 수 없습니다.")
        );
        validateRequiredLawdCodes();
    }

    private List<String> parseLawdCodes(String lawdCodes) {
        if (lawdCodes.isBlank()) {
            return List.of();
        }

        String[] candidates = lawdCodes.split(",", -1);
        List<String> parsedLawdCodes = new ArrayList<>(candidates.length);
        Set<String> uniqueLawdCodes = new HashSet<>();

        for (String candidate : candidates) {
            String lawdCode = candidate.trim();
            validateLawdCode(lawdCode);
            if (!uniqueLawdCodes.add(lawdCode)) {
                throw new IllegalArgumentException("법정 시군구 코드는 중복될 수 없습니다.");
            }
            parsedLawdCodes.add(lawdCode);
        }

        return List.copyOf(parsedLawdCodes);
    }

    private void validateLawdCode(String lawdCode) {
        if (lawdCode.isEmpty()) {
            throw new IllegalArgumentException(
                    "법정 시군구 코드에는 빈 항목이 포함될 수 없습니다."
            );
        }
        if (!LAWD_CODE_PATTERN.matcher(lawdCode).matches()) {
            throw new IllegalArgumentException("법정 시군구 코드는 ASCII 숫자 5자리여야 합니다.");
        }
    }

    private void validateRequiredLawdCodes() {
        if (!DISABLED_CRON.equals(cron) && lawdCodes.isEmpty()) {
            throw new IllegalArgumentException(
                    "활성 cron에는 법정 시군구 코드가 하나 이상 필요합니다."
            );
        }
    }
}
