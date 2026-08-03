package com.kbait.anchack.user.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

/**
 * 카카오 사용자 정보 (v2/user/me 응답에서 필요한 값만 추출)
 */
@Getter
@Setter
public class KakaoUserInfo implements Serializable {

    private Long id;
    private String nickname;
    private String profileImage;
    private String email;
}
