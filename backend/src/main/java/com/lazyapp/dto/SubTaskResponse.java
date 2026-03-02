package com.lazyapp.dto;

import com.lazyapp.model.TaskStatus;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class SubTaskResponse {
    private Long id;
    private String title;
    private String description;
    private Double estimatedHours;
    private LocalDate scheduledDate;
    private Integer sortOrder;
    private TaskStatus status;
}
