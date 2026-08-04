package com.kbait.anchack.user.dto;

import java.time.LocalDate;

public class UserUpdateRequest {

    private String nickname;
    private LocalDate birthDate;

    public UserUpdateRequest() {
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(LocalDate birthDate) {
        this.birthDate = birthDate;
    }
}