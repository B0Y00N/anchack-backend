package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.RecommendationRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecommendationMapper {

    int deleteByConditionId(@Param("conditionId") Long conditionId);

    int insertBatch(@Param("rows") List<RecommendationRow> rows);
}
