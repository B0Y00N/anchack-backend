package com.kbait.anchack.user.mapper;

import com.kbait.anchack.user.domain.User;
import org.apache.ibatis.annotations.Param;

public interface UserMapper {

    User findByProviderAndProviderId(
        @Param("provider") String provider,
        @Param("providerId") String providerId
    );

    User findById(
        @Param("userId") Long userId
    );

    int insert(
        User user
    );

    int updateProfile(
        User user
    );

    int updateSocialProfile(
        User user
    );

    int updateLastLoginAt(
        @Param("userId") Long userId
    );
}
