package com.wardk.meeteam_backend.domain.llm.service;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTask;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * LLM 태스크 오케스트레이션을 담당하는 인터페이스
 */
public interface LlmOrchestrator {

    /**
     * PR 리뷰 작업을 비동기적으로 시작합니다.
     * 
     * @param reviewJob PR 리뷰 작업
     * @return 완료를 나타내는 CompletableFuture
     */
    CompletableFuture<Void> startPrReview(PrReviewJob reviewJob);
    
    /**
     * PR 리뷰 작업에 대한 파일 리뷰 태스크를 생성합니다.
     * 
     * @param reviewJob PR 리뷰 작업
     * @return 생성된 오케스트레이션 태스크
     */
    LlmTask createFileReviewTask(PrReviewJob reviewJob, PullRequestFile pullRequestFile);
    
    /**
     * 태스크 상태를 조회합니다.
     * 
     * @param taskId 태스크 ID
     * @return 태스크 상태 정보
     */
    LlmTaskStatus getTaskStatus(Long taskId);
    
    /**
     * 태스크 상태 정보 DTO
     */
    @Data
    @Builder
    class LlmTaskStatus {
        private Long taskId;
        private Integer prNumber;
        private String status;
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private Integer totalChildTasks;
        private Integer completedChildTasks;
        private Integer failedChildTasks;
        private Integer runningChildTasks;
        private Integer progressPercentage;
    }
}
