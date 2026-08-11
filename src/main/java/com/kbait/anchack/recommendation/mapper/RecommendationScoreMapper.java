package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.RecommendationScoreRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecommendationScoreMapper {

    int deleteByConditionId(@Param("conditionId") Long conditionId);

    int insertBatch(@Param("rows") List<RecommendationScoreRow> rows);
}
