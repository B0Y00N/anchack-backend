package com.kbait.anchack.place.client;

import com.kbait.anchack.place.domain.PlaceCategory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public final class KakaoPlaceCollectionTargetProvider {

    private static final long KAKAO_DATA_SOURCE_ID = 13L;

    private static final List<SeoulGuSearchCenter> SEOUL_GU_SEARCH_CENTERS = List.of(
            new SeoulGuSearchCenter("11010", "종로구", "37.573505", "126.978988"),
            new SeoulGuSearchCenter("11020", "중구", "37.563646", "126.997553"),
            new SeoulGuSearchCenter("11030", "용산구", "37.532600", "126.990000"),
            new SeoulGuSearchCenter("11040", "성동구", "37.563400", "127.036900"),
            new SeoulGuSearchCenter("11050", "광진구", "37.538400", "127.082300"),
            new SeoulGuSearchCenter("11060", "동대문구", "37.574400", "127.039600"),
            new SeoulGuSearchCenter("11070", "중랑구", "37.606300", "127.092500"),
            new SeoulGuSearchCenter("11080", "성북구", "37.589400", "127.016700"),
            new SeoulGuSearchCenter("11090", "강북구", "37.639600", "127.025700"),
            new SeoulGuSearchCenter("11100", "도봉구", "37.668800", "127.047100"),
            new SeoulGuSearchCenter("11110", "노원구", "37.654200", "127.056800"),
            new SeoulGuSearchCenter("11120", "은평구", "37.602700", "126.929100"),
            new SeoulGuSearchCenter("11130", "서대문구", "37.579100", "126.936800"),
            new SeoulGuSearchCenter("11140", "마포구", "37.566300", "126.901900"),
            new SeoulGuSearchCenter("11150", "양천구", "37.517000", "126.866500"),
            new SeoulGuSearchCenter("11160", "강서구", "37.550900", "126.849500"),
            new SeoulGuSearchCenter("11170", "구로구", "37.495400", "126.887400"),
            new SeoulGuSearchCenter("11180", "금천구", "37.456900", "126.895500"),
            new SeoulGuSearchCenter("11190", "영등포구", "37.526400", "126.896200"),
            new SeoulGuSearchCenter("11200", "동작구", "37.512400", "126.939300"),
            new SeoulGuSearchCenter("11210", "관악구", "37.478400", "126.951600"),
            new SeoulGuSearchCenter("11220", "서초구", "37.483600", "127.032700"),
            new SeoulGuSearchCenter("11230", "강남구", "37.517300", "127.047300"),
            new SeoulGuSearchCenter("11240", "송파구", "37.514500", "127.105900"),
            new SeoulGuSearchCenter("11250", "강동구", "37.530100", "127.123800")
    );

    private static final List<KakaoPlaceSearchSpec> SEARCH_SPECS = List.of(
            category("MT1", PlaceCategory.MART),
            category("CS2", PlaceCategory.CONVENIENCE_STORE),
            category("BK9", PlaceCategory.BANK),
            category("FD6", PlaceCategory.RESTAURANT),
            category("CE7", PlaceCategory.CAFE),
            category("HP8", PlaceCategory.HOSPITAL),
            category("PM9", PlaceCategory.PHARMACY),
            category("SW8", PlaceCategory.SUBWAY_STATION),
            keyword("하천", PlaceCategory.RIVER, "여행 > 관광,명소 > 하천"),
            keyword("등산로", PlaceCategory.TRAIL, "여행 > 관광,명소 > 등산로"),
            keyword("헬스장", PlaceCategory.GYM, "스포츠,레저 > 스포츠시설 > 헬스클럽"),
            keyword("백화점", PlaceCategory.DEPARTMENT_STORE, "가정,생활 > 백화점")
    );

    public List<PlaceCollectionTarget> createTargets() {
        List<PlaceCollectionTarget> targets = new ArrayList<>();
        for (SeoulGuSearchCenter guSearchCenter : SEOUL_GU_SEARCH_CENTERS) {
            for (KakaoPlaceSearchSpec searchSpec : SEARCH_SPECS) {
                targets.add(createTarget(guSearchCenter, searchSpec));
            }
        }

        return List.copyOf(targets);
    }

    private PlaceCollectionTarget createTarget(
            SeoulGuSearchCenter guSearchCenter,
            KakaoPlaceSearchSpec searchSpec
    ) {
        return PlaceCollectionTarget.builder()
                .dataSourceId(KAKAO_DATA_SOURCE_ID)
                .guCode(guSearchCenter.guCode)
                .guName(guSearchCenter.guName)
                .searchType(searchSpec.searchType)
                .requestValue(searchSpec.requestValue)
                .placeCategory(searchSpec.placeCategory)
                .expectedCategoryName(searchSpec.expectedCategoryName)
                .centerLatitude(guSearchCenter.centerLatitude)
                .centerLongitude(guSearchCenter.centerLongitude)
                .build();
    }

    private static KakaoPlaceSearchSpec category(
            String categoryGroupCode,
            PlaceCategory placeCategory
    ) {
        return new KakaoPlaceSearchSpec(
                KakaoPlaceSearchType.CATEGORY,
                categoryGroupCode,
                placeCategory,
                null
        );
    }

    private static KakaoPlaceSearchSpec keyword(
            String keyword,
            PlaceCategory placeCategory,
            String expectedCategoryName
    ) {
        return new KakaoPlaceSearchSpec(
                KakaoPlaceSearchType.KEYWORD,
                keyword,
                placeCategory,
                expectedCategoryName
        );
    }

    private static final class SeoulGuSearchCenter {

        private final String guCode;
        private final String guName;
        private final BigDecimal centerLatitude;
        private final BigDecimal centerLongitude;

        private SeoulGuSearchCenter(
                String guCode,
                String guName,
                String centerLatitude,
                String centerLongitude
        ) {
            this.guCode = guCode;
            this.guName = guName;
            this.centerLatitude = new BigDecimal(centerLatitude);
            this.centerLongitude = new BigDecimal(centerLongitude);
        }
    }

    private static final class KakaoPlaceSearchSpec {

        private final KakaoPlaceSearchType searchType;
        private final String requestValue;
        private final PlaceCategory placeCategory;
        private final String expectedCategoryName;

        private KakaoPlaceSearchSpec(
                KakaoPlaceSearchType searchType,
                String requestValue,
                PlaceCategory placeCategory,
                String expectedCategoryName
        ) {
            this.searchType = searchType;
            this.requestValue = requestValue;
            this.placeCategory = placeCategory;
            this.expectedCategoryName = expectedCategoryName;
        }
    }
}
