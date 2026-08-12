package com.kbait.anchack.user.service.impl;

import com.kbait.anchack.user.domain.User;
import com.kbait.anchack.user.dto.request.UserUpdateRequest;
import com.kbait.anchack.user.dto.response.UserResponse;
import com.kbait.anchack.user.exception.UserNotFoundException;
import com.kbait.anchack.user.mapper.UserMapper;
import com.kbait.anchack.user.service.UserService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;

    public UserServiceImpl(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getMyProfile(Long userId) {
        return UserResponse.from(getUser(userId));
    }

    @Override
    @Transactional
    public UserResponse updateMyProfile(Long userId, UserUpdateRequest request) {
        User user = getUser(userId);

        applyUpdate(user, request);

        int updatedCount = userMapper.updateProfile(user);

        if (updatedCount != 1) {
            throw new IllegalStateException("프로필 수정에 실패했습니다.");
        }

        return UserResponse.from(getUser(userId));
    }

    private User getUser(Long userId) {
        User user = userMapper.findById(userId);

        if (user == null) {
            throw new UserNotFoundException(userId);
        }

        return user;
    }

    /**
     * 요청에 포함된 프로필 항목만 사용자 객체에 반영한다.
     */
    private void applyUpdate(User user, UserUpdateRequest request) {
        if (request.getNickname() != null) {
            user.setNickname(request.getNickname().trim());
        }

        if (request.getBirthDate() != null) {
            user.setBirthDate(request.getBirthDate());
        }

        if (request.getProfileImageUrl() != null) {
            user.setProfileImageUrl(request.getProfileImageUrl().trim());
        }
    }
}
