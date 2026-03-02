package com.lazyapp.dto;

import com.lazyapp.model.TaskStatus;
import com.lazyapp.model.Urgency;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class TaskResponse {
    private Long id;
    private String title;
    private String description;
    private Urgency urgency;
    private LocalDate deadline;
    private TaskStatus status;
    private List<SubTaskResponse> subTasks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
