package com.kbait.anchack.condition.mapper;

import com.kbait.anchack.condition.dto.ConditionEssentialRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ConditionEssentialMapper {

    int insertBatch(@Param("rows") List<ConditionEssentialRow> rows);
}
