package com.kbait.anchack.user.mapper;

import com.kbait.anchack.user.domain.User;
import org.apache.ibatis.annotations.Param;

/**
 * 사용자(마이페이지) 관련 Mapper
 */
public interface UserMapper {

    /**
     * 사용자 ID로 조회
     *
     * @param id 사용자 PK
     * @return 사용자 정보
     */
    User findById(@Param("id") Long id);

    /**
     * 사용자 프로필 수정
     *
     * @param user 수정할 사용자 정보
     * @return 수정된 행 수
     */
    int updateProfile(User user);

}