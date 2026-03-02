package com.lazyapp.controller;

import com.lazyapp.dto.TaskRequest;
import com.lazyapp.dto.TaskResponse;
import com.lazyapp.model.TaskStatus;
import com.lazyapp.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class TaskController {

    private final TaskService taskService;

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId,
            @Valid @RequestBody TaskRequest request) {
        TaskResponse response = taskService.createTask(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getActiveTasks(
            @RequestHeader(value = "X-User-Id", defaultValue = "1") Long userId) {
        return ResponseEntity.ok(taskService.getActiveTasks(userId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTask(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTask(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<TaskResponse> updateTaskStatus(
            @PathVariable Long id,
            @RequestParam TaskStatus status) {
        return ResponseEntity.ok(taskService.updateTaskStatus(id, status));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
