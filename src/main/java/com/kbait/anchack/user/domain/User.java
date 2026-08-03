package com.kbait.anchack.user.domain;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * users 테이블 매핑 (로컬/카카오 회원 공용)
 */
@Getter
@Setter
public class User implements Serializable {

    private Long id;
    private String email;
    private String password;
    private String name;
    private String nickname;
    private LocalDate birthDate;
    private String gender;
    private String profileImageUrl;
    private String provider;
    private String providerId;
    private String status;
    private String role;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
}
