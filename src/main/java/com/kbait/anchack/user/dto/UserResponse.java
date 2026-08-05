package com.kbait.anchack.user.dto.response;

import com.kbait.anchack.user.domain.User;
import lombok.Getter;

@Getter
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
}
