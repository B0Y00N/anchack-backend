package com.kbait.anchack.auth.dto;

import java.io.Serializable;
import java.time.LocalDate;

/**
 * 카카오 사용자 정보 API(GET /v2/user/me) 응답에서 필요한 값만 추출한 DTO.
 *
 * 카카오 API의 원본 JSON 구조를 그대로 노출하지 않고, 내부에서 필요한
 * 필드만 담아 카카오 응답 형식이 바뀌어도 영향 범위를 이 클래스로 한정한다.
 */
public class KakaoUserInfo implements Serializable {

    private static final long serialVersionUID = 1L;

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
