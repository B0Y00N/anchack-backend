package com.kbait.anchack.user.dto.response;

import com.kbait.anchack.user.domain.User;

/**
 * User 도메인 객체를 API 응답으로 그대로 내려보내지 않기 위한 응답 전용 DTO
 * (password 등 민감/내부 필드 노출 방지)
 */
public class UserResponse {

    private Long id;
    private String email;
    private String nickname;
    private String profileImageUrl;
    private String role;

    public static UserResponse from(User user) {
        UserResponse response = new UserResponse();
        response.id = user.getId();
        response.email = user.getEmail();
        response.nickname = user.getNickname();
        response.profileImageUrl = user.getProfileImageUrl();
        response.role = user.getRole();
        return response;
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getRole() {
        return role;
    }
}
