package com.kbait.anchack.route.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class CommuteResult {

    private Integer commuteTime;
    private Integer transferCount;
}
