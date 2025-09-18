package com.wardk.meeteam_backend.domain.llm.repository;

import com.wardk.meeteam_backend.domain.llm.entity.LlmTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LlmTaskRepository extends JpaRepository<LlmTask, Long> {

    List<LlmTask> findByPrReviewJobId(Long reviewJobId);

    @Query("""
            SELECT t
            FROM LlmTask t
            WHERE t.status = :status
            ORDER BY t.priority DESC, t.createdAt ASC
            """)
    List<LlmTask> findByStatusOrderByPriorityAndCreatedAt(@Param("status") LlmTask.TaskStatus status);

    @Modifying
    @Query("""
        UPDATE LlmTask t 
        SET t.status = :status, t.completedAt = CURRENT_TIMESTAMP 
        WHERE t.id = :taskId
        """)
    void updateTaskStatus(@Param("taskId") Long taskId, @Param("status") LlmTask.TaskStatus status);

    List<LlmTask> findByPrReviewJobIdAndTaskType(Long reviewJobId, LlmTask.TaskType taskType);
}
