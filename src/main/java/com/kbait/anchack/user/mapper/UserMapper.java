package com.kbait.anchack.user.mapper;

import com.kbait.anchack.user.domain.User;
import org.apache.ibatis.annotations.Param;

/**
 * 마이페이지 프로필 조회/수정에 사용하는 Mapper.
 *
 * 카카오 로그인 처리(사용자 생성/소셜 프로필 갱신)는
 * auth.mapper.KakaoUserMapper가 담당한다.
 */
public interface UserMapper {

    User findById(
        @Param("userId") Long userId
    );

    int updateProfile(
        User user
    );
}
