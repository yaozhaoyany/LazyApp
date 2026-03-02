package com.lazyapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class DeadlineConflict {
    private Long taskId;
    private String taskTitle;
    private LocalDate deadline;
    private String reason;
}
