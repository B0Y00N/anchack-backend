package com.kbait.anchack.auth.mapper;

import com.kbait.anchack.auth.domain.AuthUser;
import org.apache.ibatis.annotations.Param;

public interface KakaoUserMapper {

    AuthUser findByProviderAndProviderId(
            @Param("provider") String provider,
            @Param("providerId") String providerId
    );

    AuthUser findById(
            @Param("id") Long id
    );

    int insert(AuthUser user);

    int updateSocialProfile(AuthUser user);

    int updateLastLoginAt(
            @Param("id") Long id
    );
}
