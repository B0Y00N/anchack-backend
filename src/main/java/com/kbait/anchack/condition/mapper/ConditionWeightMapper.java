package com.kbait.anchack.condition.mapper;

import com.kbait.anchack.condition.dto.ConditionWeightRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConditionWeightMapper {

    int insertBatch(@Param("rows") List<ConditionWeightRow> rows);

    List<ConditionWeightRow> findByConditionId(@Param("conditionId") Long conditionId);
}
