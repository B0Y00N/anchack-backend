package com.kbait.anchack.review.dto.request;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class AdminReviewStatusRequest {

    private String status;
    private String reason;
}
