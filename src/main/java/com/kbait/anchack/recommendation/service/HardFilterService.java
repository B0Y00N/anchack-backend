package com.kbait.anchack.recommendation.service;

import com.kbait.anchack.recommendation.dto.ConditionBundle;
import com.kbait.anchack.recommendation.dto.HardFilterResult;

public interface HardFilterService {

    HardFilterResult filter(ConditionBundle condition);
}
