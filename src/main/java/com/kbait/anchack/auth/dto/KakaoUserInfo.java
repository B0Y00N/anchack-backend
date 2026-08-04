package com.kbait.anchack.auth.dto;

import java.io.Serializable;
import java.time.LocalDate;

public class KakaoUserInfo implements Serializable {

    private Long id;
    private String nickname;
    private String profileImage;
    private String email;

    private String birthYear;
    private String birthday;
    private LocalDate birthDate;

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

    // profileImage와 동일한 값을 반환
    public String getProfileImageUrl() {
        return profileImage;
    }

    // profileImage와 동일한 필드에 저장
    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImage = profileImageUrl;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getBirthYear() {
        return birthYear;
    }

    public void setBirthYear(String birthYear) {
        this.birthYear = birthYear;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}