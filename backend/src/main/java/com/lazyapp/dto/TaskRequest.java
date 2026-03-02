package com.lazyapp.dto;

import com.lazyapp.model.Urgency;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskRequest {

    @NotBlank(message = "任务标题不能为空")
    private String title;

    private String description;

    private Urgency urgency;

    private LocalDate deadline;
}
