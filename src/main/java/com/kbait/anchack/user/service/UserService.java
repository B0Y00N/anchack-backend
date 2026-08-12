package com.kbait.anchack.user.service;

import com.kbait.anchack.user.dto.request.UserUpdateRequest;
import com.kbait.anchack.user.dto.response.UserResponse;

/**
 * 마이페이지 프로필 조회/수정을 담당하는 서비스.
 */
public interface UserService {

    /**
     * 로그인한 사용자의 프로필을 조회한다. 없으면 예외를 던진다.
     */
    UserResponse getMyProfile(Long userId);

    /**
     * 로그인한 사용자의 프로필을 수정한다. 요청에 담긴 항목만 반영한다.
     */
    UserResponse updateMyProfile(Long userId, UserUpdateRequest request);
}
