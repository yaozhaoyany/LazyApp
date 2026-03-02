package com.lazyapp.controller;

import com.lazyapp.dto.AIMessageRequest;
import com.lazyapp.dto.DailyPlanResponse;
import com.lazyapp.model.Task;
import com.lazyapp.model.SubTask;
import com.lazyapp.model.TaskStatus;
import com.lazyapp.repository.SubTaskRepository;
import com.lazyapp.repository.TaskRepository;
import com.lazyapp.service.AIService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class AIController {

    private final AIService aiService;
    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;

    @PostMapping("/tasks/{taskId}/decompose")
    public ResponseEntity<List<SubTask>> decomposeTask(@PathVariable Long taskId) {
        log.info("[AI] 拆解任务 taskId={}", taskId);
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        List<SubTask> subTasks = aiService.decomposeTask(task);
        log.info("[AI] 拆解完成 taskId={}, 子任务数={}", taskId, subTasks.size());
        return ResponseEntity.ok(subTasks);
    }

    @PostMapping("/tasks/{taskId}/clarify")
    public ResponseEntity<Map<String, String>> askClarification(@PathVariable Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        String response = aiService.askClarification(task);
        return ResponseEntity.ok(Map.of("message", response));
    }

    @PostMapping("/tasks/{taskId}/respond")
    public ResponseEntity<Map<String, String>> respondToClarification(
            @PathVariable Long taskId,
            @Valid @RequestBody AIMessageRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        String response = aiService.respondToClarification(task, request.getMessage());
        return ResponseEntity.ok(Map.of("message", response));
    }

    @PostMapping("/plan/daily")
    public ResponseEntity<DailyPlanResponse> generateDailyPlan(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        log.info("[AI] 生成日程 userId={}", userId);
        DailyPlanResponse response = aiService.generateDailyPlan(userId, false);
        log.info("[AI] 日程生成完成 userId={}, planLength={}, conflicts={}", userId,
                response.getPlan() != null ? response.getPlan().length() : 0,
                response.getDeadlineConflicts() != null ? response.getDeadlineConflicts().size() : 0);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/plan/skip-day")
    public ResponseEntity<DailyPlanResponse> skipDay(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        log.info("[AI] 摸鱼 userId={}", userId);
        DailyPlanResponse response = aiService.generateDailyPlan(userId, true);
        log.info("[AI] 摸鱼计划生成完成 userId={}, conflicts={}", userId,
                response.getDeadlineConflicts() != null ? response.getDeadlineConflicts().size() : 0);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/subtasks/{subTaskId}/clarify")
    public ResponseEntity<Map<String, String>> clarifySubTask(@PathVariable Long subTaskId) {
        log.info("[AI] 子任务细化 subTaskId={}", subTaskId);
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("子任务不存在"));
        String response = aiService.clarifySubTask(subTask);
        log.info("[AI] 子任务细化完成 subTaskId={}, responseLength={}", subTaskId, response.length());
        return ResponseEntity.ok(Map.of("message", response));
    }

    @PatchMapping("/subtasks/{subTaskId}/status")
    public ResponseEntity<Map<String, String>> updateSubTaskStatus(
            @PathVariable Long subTaskId,
            @RequestParam TaskStatus status) {
        SubTask subTask = subTaskRepository.findById(subTaskId)
                .orElseThrow(() -> new RuntimeException("子任务不存在"));
        subTask.setStatus(status);
        subTaskRepository.save(subTask);
        return ResponseEntity.ok(Map.of("message", "子任务状态已更新"));
    }

    @PatchMapping("/tasks/{taskId}/deadline")
    public ResponseEntity<Map<String, String>> updateDeadline(
            @PathVariable Long taskId,
            @RequestParam String newDeadline) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        task.setDeadline(LocalDate.parse(newDeadline));
        taskRepository.save(task);
        return ResponseEntity.ok(Map.of("message", "截止日期已更新为 " + newDeadline));
    }
}
