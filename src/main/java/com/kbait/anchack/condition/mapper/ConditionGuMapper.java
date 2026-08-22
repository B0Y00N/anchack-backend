package com.kbait.anchack.condition.mapper;

import com.kbait.anchack.condition.dto.ConditionGuRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConditionGuMapper {

    int insertBatch(@Param("rows") List<ConditionGuRow> rows);

    List<String> findGuCodesByConditionId(@Param("conditionId") Long conditionId);

    int deleteByConditionId(@Param("conditionId") Long conditionId);
}
