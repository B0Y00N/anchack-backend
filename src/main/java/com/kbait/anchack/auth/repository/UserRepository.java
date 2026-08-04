package com.kbait.anchack.repository;

import com.kbait.anchack.user.domain.User;
import com.kbait.anchack.auth.mapper.KakaoUserMapper;
import org.springframework.stereotype.Repository;

@Repository
public class UserRepository {

    private final KakaoUserMapper userMapper;

    public UserRepository(KakaoUserMapper userMapper) {
        this.userMapper = userMapper;
    }

    public User findByProviderAndProviderId(
            String provider,
            String providerId
    ) {
        return userMapper.findByProviderAndProviderId(
                provider,
                providerId
        );
    }

    public User findById(Long id) {
        return userMapper.findById(id);
    }

    public void insert(User user) {
        userMapper.insert(user);
    }

    public void updateSocialProfile(User user) {
        userMapper.updateSocialProfile(user);
    }

    public void updateLastLoginAt(Long id) {
        userMapper.updateLastLoginAt(id);
    }
}