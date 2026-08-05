package com.kbait.anchack.auth.domain;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 소셜 로그인 인증 사용자 정보.
 *
 * users 테이블 중 카카오 로그인과 인증에 필요한 컬럼을 매핑한다.
 */
public class AuthUser implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * users.user_id
     */
    private Long id;

    /**
     * 소셜 로그인 제공자.
     * 예: KAKAO
     */
    private String provider;

    /**
     * 소셜 로그인 제공자가 발급한 사용자 고유 ID.
     */
    private String providerId;

    private String nickname;

    /**
     * users.profile_image_url
     */
    private String profileImage;

    private String email;

    private LocalDate birthDate;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    private LocalDateTime lastLoginAt;

    public AuthUser() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getProviderId() {
        return providerId;
    }

    public void setProviderId(String providerId) {
        this.providerId = providerId;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public String getProfileImage() {
        return profileImage;
    }

    public void setProfileImage(String profileImage) {
        this.profileImage = profileImage;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public void setLastLoginAt(LocalDateTime lastLoginAt) {
        this.lastLoginAt = lastLoginAt;
    }

    @Override
    public String toString() {
        return "AuthUser{" +
            "id=" + id +
            ", provider='" + provider + '\'' +
            ", providerId='" + providerId + '\'' +
            ", nickname='" + nickname + '\'' +
            ", profileImage='" + profileImage + '\'' +
            ", email='" + email + '\'' +
            ", birthDate=" + birthDate +
            ", createdAt=" + createdAt +
            ", updatedAt=" + updatedAt +
            ", lastLoginAt=" + lastLoginAt +
            '}';
    }
}

