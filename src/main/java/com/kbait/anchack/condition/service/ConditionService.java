package com.kbait.anchack.condition.service;

import com.kbait.anchack.condition.dto.UserConditionCreateRequest;
import com.kbait.anchack.condition.dto.UserConditionCreateResponse;

public interface ConditionService {

    UserConditionCreateResponse createAndRecommend(Long userId, UserConditionCreateRequest request);
}
