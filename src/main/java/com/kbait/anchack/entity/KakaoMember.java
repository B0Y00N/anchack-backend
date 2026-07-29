package com.kbait.anchack.entity;

import java.io.Serializable;

/**
 * DB에 저장되는 카카오 회원 정보 (kakao_member 테이블 매핑)
 */
public class KakaoMember implements Serializable {

    private Long id;
    private Long kakaoId;
    private String nickname;
    private String profileImage;
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getKakaoId() {
        return kakaoId;
    }

    public void setKakaoId(Long kakaoId) {
        this.kakaoId = kakaoId;
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
}

