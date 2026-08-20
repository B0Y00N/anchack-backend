package com.kbait.anchack.favorite.dto.response;

import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class FavoriteDongResponse {

    private Long adminDongId;
    private LocalDateTime createdAt;
}
