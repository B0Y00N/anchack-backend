package com.kbait.anchack.condition.mapper;

import com.kbait.anchack.condition.dto.PreferredHouseTypeRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PreferredHouseTypeMapper {

    int insertBatch(@Param("rows") List<PreferredHouseTypeRow> rows);

    List<String> findHouseTypesByConditionId(@Param("conditionId") Long conditionId);

    int deleteByConditionId(@Param("conditionId") Long conditionId);
}
