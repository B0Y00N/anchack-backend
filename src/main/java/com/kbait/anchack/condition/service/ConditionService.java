package com.kbait.anchack.condition.service;

import com.kbait.anchack.condition.dto.RecommendedDongResponse;
import com.kbait.anchack.condition.dto.SavedConditionResponse;
import com.kbait.anchack.condition.dto.UserConditionCreateRequest;
import com.kbait.anchack.condition.dto.UserConditionCreateResponse;

import java.util.List;

public interface ConditionService {

    UserConditionCreateResponse createAndRecommend(Long userId, UserConditionCreateRequest request);

    void saveCondition(Long userId, Long conditionId, String title);

    void unsaveCondition(Long userId, Long conditionId);

    List<SavedConditionResponse> getSavedConditions(Long userId);

    List<RecommendedDongResponse> getRecommendations(Long userId, Long conditionId);
}
