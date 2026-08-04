package com.kbait.anchack.auth.mapper;

import com.kbait.anchack.user.domain.User;
import org.apache.ibatis.annotations.Param;

public interface KakaoUserMapper {

    User findByProviderAndProviderId(
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );

    User findById(@Param("id") Long id);

    int insert(User user);

    int updateSocialProfile(User user);

    int updateLastLoginAt(@Param("id") Long id);
}