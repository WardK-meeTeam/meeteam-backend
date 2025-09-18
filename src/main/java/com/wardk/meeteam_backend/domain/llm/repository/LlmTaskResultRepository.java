package com.wardk.meeteam_backend.domain.llm.repository;

import com.wardk.meeteam_backend.domain.llm.entity.LlmTaskResult;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmTaskResultRepository extends JpaRepository<LlmTaskResult, Long> {
    
}
