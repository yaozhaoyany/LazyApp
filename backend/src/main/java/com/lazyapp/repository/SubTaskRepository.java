package com.lazyapp.repository;

import com.lazyapp.model.SubTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface SubTaskRepository extends JpaRepository<SubTask, Long> {
    List<SubTask> findByTaskIdOrderBySortOrderAsc(Long taskId);
    List<SubTask> findByTaskUserIdAndScheduledDateOrderBySortOrderAsc(Long userId, LocalDate date);
}
