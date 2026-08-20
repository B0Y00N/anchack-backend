package com.kbait.anchack.favorite.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@NoArgsConstructor
public class FavoriteDongRequest {

    @NotNull
    private Long adminDongId;
}
