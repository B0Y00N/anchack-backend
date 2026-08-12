package com.kbait.anchack.user.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
public class UserUpdateRequest {

    private String nickname;
    private LocalDate birthDate;
    private String profileImageUrl;
}
