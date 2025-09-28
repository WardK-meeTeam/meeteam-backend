package com.wardk.meeteam_backend.domain.llm.entity;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import jakarta.persistence.Version;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class LlmTask extends BaseEntity {

    public enum TaskType {
        ORCHESTRATION,
        FILE_REVIEW, // 파일 코드 리뷰
        PR_SUMMARY, // PR 전체 요약
        CODE_EXPLANATION // 코드 설명
    }

    public enum TaskStatus {
        CREATED, // 생성됨
        QUEUED, // 큐에 등록됨
        RUNNING, // 실행 중
        COMPLETED, // 성공적으로 완료됨
        FAILED // 실패함
    }

    public enum Priority {
        LOW,
        NORMAL,
        HIGH,
        CRITICAL
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* 리뷰 작업 */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pr_review_job_id")
    private PrReviewJob prReviewJob;

    /* 대상 파일 ID (FILE_REVIEW 타입일 때만 사용) */
    @Column(name = "pull_request_file_id")
    private Long pullRequestFileId;

    @Column(name = "pull_request_file_name")
    private String pullRequestFileName;

    @OneToOne(cascade = CascadeType.ALL, orphanRemoval = true)
    @JoinColumn(name = "llm_task_result_id", unique = false) // unique 제약조건 제거
    private LlmTaskResult llmTaskResult;

    /* 태스크 유형 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskType taskType;

    /* 태스크 상태 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Version
    private Long version;

    /* 우선순위 */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;

    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    public void updateStatus(TaskStatus newStatus) {
        this.status = newStatus;

        if (newStatus == TaskStatus.RUNNING && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }

        if ((newStatus == TaskStatus.COMPLETED || newStatus == TaskStatus.FAILED)
                && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    public void completeWithSuccess(LlmTaskResult result) {
        this.status = TaskStatus.COMPLETED;
        this.completedAt = LocalDateTime.now();

        this.llmTaskResult = result;
    }

    public void completeWithError(String errorMessage) {
        this.status = TaskStatus.FAILED;
        this.completedAt = LocalDateTime.now();
        this.errorMessage = errorMessage;
    }

    public boolean isCompleted() {
        return this.status == TaskStatus.COMPLETED || this.status == TaskStatus.FAILED;
    }

    /**
     * 대상 파일을 조회합니다 (FILE_REVIEW 타입일 때만 유효)
     */
    public PullRequestFile getPullRequestFile() {
        if (pullRequestFileId == null || prReviewJob == null) {
            return null;
        }

        return prReviewJob.getPullRequest().getFiles().stream()
                .filter(file -> file.getId().equals(pullRequestFileId))
                .findFirst()
                .orElse(null);
    }
}