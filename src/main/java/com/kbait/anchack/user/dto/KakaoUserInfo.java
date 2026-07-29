package com.kbait.anchack.user.dto;

import java.io.Serializable;

/**
 * 카카오 사용자 정보 (v2/user/me 응답에서 필요한 값만 추출)
 */
public class KakaoUserInfo implements Serializable {

    private Long id;
    private String nickname;
    private String profileImage;
    private String email;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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
