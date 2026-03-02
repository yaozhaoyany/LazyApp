package com.lazyapp.repository;

import com.lazyapp.model.Task;
import com.lazyapp.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<Task> findByUserIdAndStatusOrderByUrgencyDescDeadlineAsc(Long userId, TaskStatus status);
    List<Task> findByUserIdAndStatusNotOrderByUrgencyDescDeadlineAsc(Long userId, TaskStatus status);
}
