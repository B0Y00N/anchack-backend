package com.kbait.anchack.admindong.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RentDistBucket {

    private String label;
    private int count;
}
