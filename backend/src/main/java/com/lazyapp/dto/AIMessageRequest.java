package com.lazyapp.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AIMessageRequest {

    @NotBlank(message = "消息内容不能为空")
    private String message;
}
