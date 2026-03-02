package com.lazyapp.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class DailyPlanResponse {
    private String plan;
    private LocalDateTime generatedAt;
    private boolean skippedDay;
    private List<DeadlineConflict> deadlineConflicts;
}
