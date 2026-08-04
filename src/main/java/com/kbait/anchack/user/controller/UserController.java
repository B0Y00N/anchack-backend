package com.kbait.anchack.user.controller;

import com.kbait.anchack.common.security.JwtAuthenticationFilter;
import com.kbait.anchack.user.domain.User;
import com.kbait.anchack.user.mapper.UserMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 마이페이지(사용자 프로필) 관련 API
 *
 * JwtAuthenticationFilter를 통과한 /api/** 요청에는
 * request attribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE)로 인증된 사용자 PK가 담겨 있다.
 *
 * CORS는 WebConfig의 CorsFilter가 전역으로 처리하므로 여기서는 별도 설정하지 않는다.
 */
@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserMapper userMapper;

    public UserController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    /**
     * 내 프로필 조회
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {

        Long userId = resolveUserId(request);

        if (userId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("UNAUTHORIZED", "인증 정보가 없습니다."));
        }

        User user = userMapper.findById(userId);

        if (user == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));
        }

        return ResponseEntity.ok(user);
    }

    /**
     * 내 프로필 수정
     */
    @PutMapping("/me")
    public ResponseEntity<?> updateMyProfile(
            HttpServletRequest request,
            @RequestBody UpdateProfileRequest updateRequest
    ) {
        Long userId = resolveUserId(request);

        if (userId == null) {
            return ResponseEntity
                    .status(HttpStatus.UNAUTHORIZED)
                    .body(createErrorResponse("UNAUTHORIZED", "인증 정보가 없습니다."));
        }

        User existingUser = userMapper.findById(userId);

        if (existingUser == null) {
            return ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(createErrorResponse("USER_NOT_FOUND", "사용자 정보를 찾을 수 없습니다."));
        }

        if (updateRequest.getNickname() != null) {
            existingUser.setNickname(updateRequest.getNickname());
        }

        if (updateRequest.getProfileImage() != null) {
            existingUser.setProfileImage(updateRequest.getProfileImage());
        }

        int updatedCount = userMapper.updateProfile(existingUser);

        if (updatedCount != 1) {
            return ResponseEntity
                    .status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(createErrorResponse("PROFILE_UPDATE_FAILED", "프로필 수정에 실패했습니다."));
        }

        User updatedUser = userMapper.findById(userId);

        return ResponseEntity.ok(updatedUser);
    }

    private Long resolveUserId(HttpServletRequest request) {
        Object userIdAttribute =
                request.getAttribute(JwtAuthenticationFilter.USER_ID_ATTRIBUTE);

        if (userIdAttribute instanceof Long) {
            return (Long) userIdAttribute;
        }

        if (userIdAttribute instanceof Number) {
            return ((Number) userIdAttribute).longValue();
        }

        return null;
    }

    private Map<String, Object> createErrorResponse(String code, String message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", code);
        response.put("message", message);
        return response;
    }

    public static class UpdateProfileRequest {

        private String nickname;
        private String profileImage;

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
    }
}