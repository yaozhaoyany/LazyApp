package com.lazyapp.service;

import com.lazyapp.dto.*;
import com.lazyapp.model.*;
import com.lazyapp.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class TaskService {

    private final TaskRepository taskRepository;
    private final SubTaskRepository subTaskRepository;
    private final UserRepository userRepository;

    @Transactional
    public TaskResponse createTask(Long userId, TaskRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("用户不存在"));

        Task task = Task.builder()
                .user(user)
                .title(request.getTitle())
                .description(request.getDescription())
                .urgency(request.getUrgency() != null ? request.getUrgency() : Urgency.MEDIUM)
                .deadline(request.getDeadline())
                .status(TaskStatus.PENDING)
                .build();

        task = taskRepository.save(task);
        return toResponse(task);
    }

    public List<TaskResponse> getActiveTasks(Long userId) {
        List<Task> tasks = taskRepository.findByUserIdAndStatusNotOrderByUrgencyDescDeadlineAsc(
                userId, TaskStatus.CANCELLED);
        return tasks.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public TaskResponse getTask(Long taskId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTask(Long taskId, TaskRequest request) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));

        if (request.getTitle() != null) task.setTitle(request.getTitle());
        if (request.getDescription() != null) task.setDescription(request.getDescription());
        if (request.getUrgency() != null) task.setUrgency(request.getUrgency());
        if (request.getDeadline() != null) task.setDeadline(request.getDeadline());

        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public TaskResponse updateTaskStatus(Long taskId, TaskStatus status) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new RuntimeException("任务不存在"));
        task.setStatus(status);
        task = taskRepository.save(task);
        return toResponse(task);
    }

    @Transactional
    public void deleteTask(Long taskId) {
        taskRepository.deleteById(taskId);
    }

    private TaskResponse toResponse(Task task) {
        List<SubTaskResponse> subTaskResponses = task.getSubTasks() != null
                ? task.getSubTasks().stream().map(this::toSubTaskResponse).collect(Collectors.toList())
                : List.of();

        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .urgency(task.getUrgency())
                .deadline(task.getDeadline())
                .status(task.getStatus())
                .subTasks(subTaskResponses)
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }

    private SubTaskResponse toSubTaskResponse(SubTask subTask) {
        return SubTaskResponse.builder()
                .id(subTask.getId())
                .title(subTask.getTitle())
                .description(subTask.getDescription())
                .estimatedHours(subTask.getEstimatedHours())
                .scheduledDate(subTask.getScheduledDate())
                .sortOrder(subTask.getSortOrder())
                .status(subTask.getStatus())
                .build();
    }
}
