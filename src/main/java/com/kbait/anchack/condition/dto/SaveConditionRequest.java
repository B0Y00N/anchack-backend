package com.kbait.anchack.condition.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.validation.constraints.Size;

@Getter
@Setter
@NoArgsConstructor
public class SaveConditionRequest {

    @Size(max = 100)
    private String title;
}
