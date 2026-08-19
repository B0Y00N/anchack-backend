package com.kbait.anchack.place.client;

import com.kbait.anchack.place.domain.PlaceCategory;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;

class KakaoPlaceCollectionTargetProviderTest {

    private final KakaoPlaceCollectionTargetProvider provider = new KakaoPlaceCollectionTargetProvider();

    @Test
    void createsTargetsForEverySeoulGuAndKakaoSearchSpec() {
        List<PlaceCollectionTarget> targets = provider.createTargets();

        assertThat(targets).hasSize(300);
        assertThat(targets)
                .extracting(PlaceCollectionTarget::getGuCode)
                .contains("11010", "11210", "11250");
        assertThat(targets)
                .extracting(PlaceCollectionTarget::getPlaceCategory)
                .contains(
                        PlaceCategory.MART,
                        PlaceCategory.CONVENIENCE_STORE,
                        PlaceCategory.PHARMACY,
                        PlaceCategory.SUBWAY_STATION,
                        PlaceCategory.RIVER,
                        PlaceCategory.TRAIL,
                        PlaceCategory.GYM,
                        PlaceCategory.DEPARTMENT_STORE
                );
    }

    @Test
    void categoryTargetUsesKakaoCategoryCode() {
        List<PlaceCollectionTarget> targets = provider.createTargets();

        PlaceCollectionTarget target = findTarget(targets, "11210", PlaceCategory.PHARMACY);

        assertThat(target.getDataSourceId()).isEqualTo(13L);
        assertThat(target.getGuName()).isEqualTo("관악구");
        assertThat(target.getSearchType()).isEqualTo(KakaoPlaceSearchType.CATEGORY);
        assertThat(target.getRequestValue()).isEqualTo("PM9");
        assertThat(target.getExpectedCategoryName()).isNull();
        assertThat(target.getCenterLatitude()).isEqualTo(new BigDecimal("37.478400"));
        assertThat(target.getCenterLongitude()).isEqualTo(new BigDecimal("126.951600"));
    }

    @Test
    void keywordTargetUsesKeywordAndExpectedKakaoCategoryName() {
        List<PlaceCollectionTarget> targets = provider.createTargets();

        PlaceCollectionTarget target = findTarget(targets, "11210", PlaceCategory.GYM);

        assertThat(target.getSearchType()).isEqualTo(KakaoPlaceSearchType.KEYWORD);
        assertThat(target.getRequestValue()).isEqualTo("헬스장");
        assertThat(target.getExpectedCategoryName()).isEqualTo("스포츠,레저 > 스포츠시설 > 헬스클럽");
    }

    @Test
    void returnedTargetsCannotBeModified() {
        List<PlaceCollectionTarget> targets = provider.createTargets();

        Throwable actual = catchThrowable(() -> targets.add(targets.get(0)));

        assertThat(actual).isExactlyInstanceOf(UnsupportedOperationException.class);
    }

    private PlaceCollectionTarget findTarget(
            List<PlaceCollectionTarget> targets,
            String guCode,
            PlaceCategory placeCategory
    ) {
        return targets.stream()
                .filter(target -> target.getGuCode().equals(guCode))
                .filter(target -> target.getPlaceCategory() == placeCategory)
                .findFirst()
                .orElseThrow();
    }
}
