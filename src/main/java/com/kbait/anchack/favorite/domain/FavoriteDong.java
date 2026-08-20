package com.kbait.anchack.favorite.domain;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class FavoriteDong {

    private Long adminDongId;
    private LocalDateTime createdAt;
}
