package com.kbait.anchack.auth.repository;

import com.kbait.anchack.auth.domain.AuthUser;
import com.kbait.anchack.auth.mapper.KakaoUserMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final KakaoUserMapper userMapper;

    public UserRepository(KakaoUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public AuthUser findByProviderAndProviderId(
            String provider,
            String providerId
    ) {
        return userMapper.findByProviderAndProviderId(
                provider,
                providerId
        );
    }

    public AuthUser findById(Long id) {
        return userMapper.findById(id);
    }

    public void insert(AuthUser user) {
        userMapper.insert(user);
    }

    public void updateSocialProfile(AuthUser user) {
        userMapper.updateSocialProfile(user);
    }

    public void updateLastLoginAt(Long id) {
        userMapper.updateLastLoginAt(id);
    }
}
