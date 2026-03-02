package com.lazyapp.repository;

import com.lazyapp.model.AIConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AIConversationRepository extends JpaRepository<AIConversation, Long> {
    List<AIConversation> findByTaskIdOrderByCreatedAtAsc(Long taskId);
}
