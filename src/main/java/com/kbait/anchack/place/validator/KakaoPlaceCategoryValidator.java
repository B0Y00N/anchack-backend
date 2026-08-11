package com.kbait.anchack.place.validator;

import com.kbait.anchack.place.client.KakaoPlaceSearchType;
import com.kbait.anchack.place.client.PlaceCollectionTarget;
import com.kbait.anchack.place.dto.kakao.KakaoPlaceDocument;

public final class KakaoPlaceCategoryValidator {

    public boolean isAllowed(
            PlaceCollectionTarget target,
            KakaoPlaceDocument document
    ) {
        if (target.getSearchType() == KakaoPlaceSearchType.CATEGORY) {
            return target.getRequestValue().equals(document.getCategoryGroupCode());
        }

        return target.getExpectedCategoryName().equals(document.getCategoryName());
    }
}
