package com.kbait.anchack.user.service;

import com.kbait.anchack.auth.domain.AuthUser;
import com.kbait.anchack.auth.dto.KakaoUserInfo;
import com.kbait.anchack.auth.mapper.KakaoUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
public class UserService {

    private static final String KAKAO_PROVIDER = "KAKAO";

    private final KakaoUserMapper kakaoUserMapper;

    public UserService(
            KakaoUserMapper kakaoUserMapper
    ) {
        this.kakaoUserMapper = kakaoUserMapper;
    }

    // 카카오 로그인 사용자를 조회한다.
    // 신규 사용자면 users 테이블에 등록한다.
    // 기존 사용자면 카카오 프로필 정보와 마지막 로그인 시간을 갱신한다.
    @Transactional
    public AuthUser saveOrUpdate(
            KakaoUserInfo userInfo
    ) {
        validateKakaoUserInfo(userInfo);

        String providerId =
                String.valueOf(userInfo.getId());

        AuthUser existingUser =
                kakaoUserMapper.findByProviderAndProviderId(
                        KAKAO_PROVIDER,
                        providerId
                );

        if (existingUser == null) {
            return createKakaoUser(
                    userInfo,
                    providerId
            );
        }

        return updateKakaoUser(
                existingUser,
                userInfo
        );
    }

    // 사용자 PK로 인증 사용자 정보를 조회한다.
    // JWT 인증 완료 후 현재 로그인 사용자 조회에 사용한다.
    @Transactional(readOnly = true)
    public AuthUser findById(
            Long userId
    ) {
        if (userId == null) {
            return null;
        }

        return kakaoUserMapper.findById(userId);
    }

    // 신규 카카오 사용자를 등록한다.
    private AuthUser createKakaoUser(
            KakaoUserInfo userInfo,
            String providerId
    ) {
        AuthUser user = new AuthUser();

        user.setProvider(KAKAO_PROVIDER);
        user.setProviderId(providerId);
        user.setNickname(userInfo.getNickname());
        user.setProfileImage(userInfo.getProfileImage());
        user.setEmail(userInfo.getEmail());
        user.setBirthDate(
                convertToBirthDate(userInfo)
        );

        int insertedCount =
                kakaoUserMapper.insert(user);

        if (insertedCount != 1) {
            throw new IllegalStateException(
                    "신규 사용자 등록에 실패했습니다."
            );
        }

        if (user.getId() == null) {
            throw new IllegalStateException(
                    "신규 사용자 등록 후 사용자 ID가 생성되지 않았습니다."
            );
        }

        AuthUser savedUser =
                kakaoUserMapper.findById(
                        user.getId()
                );

        if (savedUser == null) {
            throw new IllegalStateException(
                    "등록된 사용자 정보를 조회할 수 없습니다."
            );
        }

        return savedUser;
    }

    // 기존 카카오 사용자의 프로필과 마지막 로그인 시간을 갱신한다.
    private AuthUser updateKakaoUser(
            AuthUser existingUser,
            KakaoUserInfo userInfo
    ) {
        existingUser.setNickname(
                userInfo.getNickname()
        );

        existingUser.setProfileImage(
                userInfo.getProfileImage()
        );

        existingUser.setEmail(
                userInfo.getEmail()
        );

        existingUser.setBirthDate(
                convertToBirthDate(userInfo)
        );

        int updatedProfileCount =
                kakaoUserMapper.updateSocialProfile(
                        existingUser
                );

        if (updatedProfileCount != 1) {
            throw new IllegalStateException(
                    "기존 사용자 프로필 갱신에 실패했습니다."
            );
        }

        int updatedLoginCount =
                kakaoUserMapper.updateLastLoginAt(
                        existingUser.getId()
                );

        if (updatedLoginCount != 1) {
            throw new IllegalStateException(
                    "마지막 로그인 시간 갱신에 실패했습니다."
            );
        }

        AuthUser updatedUser =
                kakaoUserMapper.findById(
                        existingUser.getId()
                );

        if (updatedUser == null) {
            throw new IllegalStateException(
                    "갱신된 사용자 정보를 조회할 수 없습니다."
            );
        }

        return updatedUser;
    }

    // 카카오 사용자 정보의 필수값을 검증한다.
    private void validateKakaoUserInfo(
            KakaoUserInfo userInfo
    ) {
        if (userInfo == null) {
            throw new IllegalArgumentException(
                    "카카오 사용자 정보가 없습니다."
            );
        }

        if (userInfo.getId() == null) {
            throw new IllegalArgumentException(
                    "카카오 사용자 ID가 없습니다."
            );
        }
    }

    // 카카오 사용자 정보의 생년월일을 LocalDate로 변환한다.
    // birthDate가 존재하면 해당 값을 우선 사용한다.
    private LocalDate convertToBirthDate(
            KakaoUserInfo userInfo
    ) {
        if (userInfo.getBirthDate() != null) {
            return userInfo.getBirthDate();
        }

        String birthYear =
                userInfo.getBirthYear();

        String birthday =
                userInfo.getBirthday();

        if (birthYear == null
                || birthYear.trim().isEmpty()
                || birthday == null
                || birthday.trim().isEmpty()) {

            return null;
        }

        String normalizedBirthYear =
                birthYear.trim();

        String normalizedBirthday =
                birthday.trim();

        if (!normalizedBirthYear.matches("\\d{4}")
                || !normalizedBirthday.matches("\\d{4}")) {

            return null;
        }

        try {
            int year =
                    Integer.parseInt(
                            normalizedBirthYear
                    );

            int month =
                    Integer.parseInt(
                            normalizedBirthday.substring(
                                    0,
                                    2
                            )
                    );

            int day =
                    Integer.parseInt(
                            normalizedBirthday.substring(
                                    2,
                                    4
                            )
                    );

            return LocalDate.of(
                    year,
                    month,
                    day
            );

        } catch (RuntimeException e) {
            return null;
        }
    }
}
