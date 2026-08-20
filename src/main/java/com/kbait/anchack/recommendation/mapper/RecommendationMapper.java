package com.kbait.anchack.recommendation.mapper;

import com.kbait.anchack.recommendation.dto.RecommendationRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface RecommendationMapper {

    int deleteByConditionId(@Param("conditionId") Long conditionId);

    int insertBatch(@Param("rows") List<RecommendationRow> rows);

    /**
     * 저장된 조건의 결과 조회용. 재계산하지 않고 이미 저장된 행을 rank순으로 그대로 읽는다.
     */
    List<RecommendationRow> findByConditionId(@Param("conditionId") Long conditionId);
}
