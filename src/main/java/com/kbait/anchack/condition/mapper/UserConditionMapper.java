package com.kbait.anchack.condition.mapper;

import com.kbait.anchack.condition.dto.UserConditionRow;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface UserConditionMapper {

    int insert(UserConditionRow row);

    UserConditionRow findById(
        @Param("conditionId") Long conditionId
    );

    /**
     * 로그인 사용자가 저장한(is_saved=true) 조건을 최신순으로 조회한다.
     */
    List<UserConditionRow> findSavedByUserId(
        @Param("userId") Long userId
    );

    int updateIsSaved(
        @Param("conditionId") Long conditionId,
        @Param("isSaved") boolean isSaved
    );

    /**
     * is_saved를 TRUE로 바꾸면서, title이 주어졌을 때만 함께 갱신한다(안 주면 기존 title 유지).
     */
    int markSaved(
        @Param("conditionId") Long conditionId,
        @Param("title") String title
    );

    /** 재계산 시 is_latest를 TRUE로 되돌린다. */
    int markLatest(
        @Param("conditionId") Long conditionId
    );

    int deleteByConditionId(
        @Param("conditionId") Long conditionId
    );
}
