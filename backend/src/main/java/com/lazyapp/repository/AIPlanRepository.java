package com.lazyapp.repository;

import com.lazyapp.model.AIPlan;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface AIPlanRepository extends JpaRepository<AIPlan, Long> {
    List<AIPlan> findByUserIdAndPlanDateOrderByGeneratedAtDesc(Long userId, LocalDate planDate);
    List<AIPlan> findByUserIdOrderByPlanDateDesc(Long userId);
}
