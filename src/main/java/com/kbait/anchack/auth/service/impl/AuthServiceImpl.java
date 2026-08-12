package com.kbait.anchack.auth.service.impl;

import com.kbait.anchack.auth.domain.AuthUser;
import com.kbait.anchack.auth.dto.KakaoUserInfo;
import com.kbait.anchack.auth.dto.response.AuthLoginResponse;
import com.kbait.anchack.auth.dto.response.AuthUserResponse;
import com.kbait.anchack.auth.mapper.KakaoUserMapper;
import com.kbait.anchack.auth.service.AuthService;
import com.kbait.anchack.auth.service.KakaoAuthService;
import com.kbait.anchack.common.exception.NotFoundException;
import com.kbait.anchack.common.security.JwtTokenProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class AuthServiceImpl implements AuthService {

    private static final String KAKAO_PROVIDER = "KAKAO";

    private final KakaoAuthService kakaoAuthService;
    private final KakaoUserMapper kakaoUserMapper;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthServiceImpl(
        KakaoAuthService kakaoAuthService,
        KakaoUserMapper kakaoUserMapper,
        JwtTokenProvider jwtTokenProvider
    ) {
        this.kakaoAuthService = kakaoAuthService;
        this.kakaoUserMapper = kakaoUserMapper;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    @Override
    @Transactional
    public AuthLoginResponse login(String code) {
        if (!StringUtils.hasText(code)) {
            throw new IllegalArgumentException("카카오 인가 코드가 없습니다.");
        }

        String kakaoAccessToken = kakaoAuthService.getAccessToken(code.trim());
        KakaoUserInfo kakaoUserInfo = kakaoAuthService.getUserInfo(kakaoAccessToken);

        AuthUser authUser = saveOrUpdateUser(kakaoUserInfo);
        String accessToken = jwtTokenProvider.generateToken(authUser);

        return new AuthLoginResponse(AuthUserResponse.from(authUser), accessToken);
    }

    @Override
    @Transactional(readOnly = true)
    public AuthUserResponse getCurrentUser(Long userId) {
        AuthUser authUser = kakaoUserMapper.findById(userId);

        if (authUser == null) {
            throw new NotFoundException("사용자 정보를 찾을 수 없습니다.");
        }

        return AuthUserResponse.from(authUser);
    }

    /**
     * 카카오 로그인 사용자를 조회한다. 신규 사용자면 등록하고,
     * 기존 사용자면 카카오 프로필과 마지막 로그인 시간을 갱신한다.
     */
    private AuthUser saveOrUpdateUser(KakaoUserInfo kakaoUserInfo) {
        String providerId = String.valueOf(kakaoUserInfo.getId());

        AuthUser existingUser =
            kakaoUserMapper.findByProviderAndProviderId(KAKAO_PROVIDER, providerId);

        if (existingUser == null) {
            return createUser(kakaoUserInfo, providerId);
        }

        return updateUser(existingUser, kakaoUserInfo);
    }

    private AuthUser createUser(KakaoUserInfo kakaoUserInfo, String providerId) {
        AuthUser user = new AuthUser();

        user.setProvider(KAKAO_PROVIDER);
        user.setProviderId(providerId);
        user.setNickname(kakaoUserInfo.getNickname());
        user.setProfileImage(kakaoUserInfo.getProfileImage());
        user.setEmail(kakaoUserInfo.getEmail());
        user.setBirthDate(kakaoUserInfo.getBirthDate());

        int insertedCount = kakaoUserMapper.insert(user);

        if (insertedCount != 1 || user.getId() == null) {
            throw new IllegalStateException("신규 사용자 등록에 실패했습니다.");
        }

        return getById(user.getId());
    }

    private AuthUser updateUser(AuthUser existingUser, KakaoUserInfo kakaoUserInfo) {
        existingUser.setNickname(kakaoUserInfo.getNickname());
        existingUser.setProfileImage(kakaoUserInfo.getProfileImage());
        existingUser.setEmail(kakaoUserInfo.getEmail());
        existingUser.setBirthDate(kakaoUserInfo.getBirthDate());

        int updatedProfileCount = kakaoUserMapper.updateSocialProfile(existingUser);

        if (updatedProfileCount != 1) {
            throw new IllegalStateException("기존 사용자 프로필 갱신에 실패했습니다.");
        }

        int updatedLoginCount = kakaoUserMapper.updateLastLoginAt(existingUser.getId());

        if (updatedLoginCount != 1) {
            throw new IllegalStateException("마지막 로그인 시간 갱신에 실패했습니다.");
        }

        return getById(existingUser.getId());
    }

    private AuthUser getById(Long userId) {
        AuthUser user = kakaoUserMapper.findById(userId);

        if (user == null) {
            throw new IllegalStateException("저장된 사용자 정보를 조회할 수 없습니다.");
        }

        return user;
    }
}
