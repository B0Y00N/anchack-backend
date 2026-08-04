package com.kbait.anchack.user.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class User implements Serializable {

    private static final long serialVersionUID = 1L;

    // 사용자 ID
    private Long id;

    // 이메일
    private String email;

    // 비밀번호
    private String password;

    // 이름
    private String name;

    // 닉네임
    private String nickname;

    // 생년월일
    private LocalDate birthDate;

    // 성별
    private String gender;

    // 프로필 이미지 URL
    private String profileImageUrl;

    // 회원 상태
    private String status;

    // 권한
    private String role;

    // 마지막 로그인 시간
    private LocalDateTime lastLoginAt;

    // 생성일
    private LocalDateTime createdAt;

    // 수정일
    private LocalDateTime updatedAt;

    // 삭제일(탈퇴)
    private LocalDateTime deletedAt;
}
