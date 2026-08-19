package com.kbait.anchack.rental.registry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class SeoulLawdCodeRegistry {

    private static final Map<String, String> GU_NAMES_BY_LAWD_CODE = createGuNamesByLawdCode();
    private static final List<String> LAWD_CODES = List.copyOf(GU_NAMES_BY_LAWD_CODE.keySet());

    public String requireGuName(String lawdCode) {
        String normalizedLawdCode = normalize(lawdCode);
        String guName = GU_NAMES_BY_LAWD_CODE.get(normalizedLawdCode);

        if (guName == null) {
            throw new IllegalArgumentException("지원하지 않는 서울 법정 시군구 코드입니다.");
        }

        return guName;
    }

    public static List<String> seoulLawdCodes() {
        return LAWD_CODES;
    }

    private static Map<String, String> createGuNamesByLawdCode() {
        Map<String, String> guNamesByLawdCode = new LinkedHashMap<>();
        guNamesByLawdCode.put("11110", "종로구");
        guNamesByLawdCode.put("11140", "중구");
        guNamesByLawdCode.put("11170", "용산구");
        guNamesByLawdCode.put("11200", "성동구");
        guNamesByLawdCode.put("11215", "광진구");
        guNamesByLawdCode.put("11230", "동대문구");
        guNamesByLawdCode.put("11260", "중랑구");
        guNamesByLawdCode.put("11290", "성북구");
        guNamesByLawdCode.put("11305", "강북구");
        guNamesByLawdCode.put("11320", "도봉구");
        guNamesByLawdCode.put("11350", "노원구");
        guNamesByLawdCode.put("11380", "은평구");
        guNamesByLawdCode.put("11410", "서대문구");
        guNamesByLawdCode.put("11440", "마포구");
        guNamesByLawdCode.put("11470", "양천구");
        guNamesByLawdCode.put("11500", "강서구");
        guNamesByLawdCode.put("11530", "구로구");
        guNamesByLawdCode.put("11545", "금천구");
        guNamesByLawdCode.put("11560", "영등포구");
        guNamesByLawdCode.put("11590", "동작구");
        guNamesByLawdCode.put("11620", "관악구");
        guNamesByLawdCode.put("11650", "서초구");
        guNamesByLawdCode.put("11680", "강남구");
        guNamesByLawdCode.put("11710", "송파구");
        guNamesByLawdCode.put("11740", "강동구");
        return Collections.unmodifiableMap(guNamesByLawdCode);
    }

    private String normalize(String lawdCode) {
        return lawdCode == null ? null : lawdCode.trim();
    }
}
