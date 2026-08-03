package com.kbait.anchack.user.service;

import com.kbait.anchack.user.dto.KakaoUserInfo;
import com.kbait.anchack.user.domain.User;
import com.kbait.anchack.user.mapper.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private static final String PROVIDER_KAKAO = "KAKAO";

    @Autowired
    private UserRepository userRepository;

    /**
     * 카카오 로그인 사용자 정보를 users 테이블에 저장(최초 로그인) 또는 갱신(기존 회원)한다.
     */
    @Transactional
    public User saveOrUpdate(KakaoUserInfo userInfo) {
        String providerId = String.valueOf(userInfo.getId());
        User existing = userRepository.findByProviderAndProviderId(PROVIDER_KAKAO, providerId);

        User user = new User();
        user.setProvider(PROVIDER_KAKAO);
        user.setProviderId(providerId);
        user.setNickname(userInfo.getNickname());
        user.setProfileImageUrl(userInfo.getProfileImage());
        user.setEmail(userInfo.getEmail());

        if (existing == null) {
            userRepository.insert(user);
            return userRepository.findByProviderAndProviderId(PROVIDER_KAKAO, providerId);
        } else {
            userRepository.update(user);
            user.setId(existing.getId());
            return user;
        }
    }
}
