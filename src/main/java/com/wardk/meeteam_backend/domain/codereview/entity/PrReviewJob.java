package com.wardk.meeteam_backend.domain.codereview.entity;

import com.wardk.meeteam_backend.domain.chat.entity.ChatRoom;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import com.wardk.meeteam_backend.domain.llm.entity.LlmTask;
import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;

/**
 * PR 리뷰 작업 엔티티
 * 
 * <p>하나의 PR에 대한 코드 리뷰 작업을 관리합니다. PR의 head SHA가 변경될 때마다
 * 새로운 리뷰 작업이 생성되며, 각 작업은 여러 개의 hunk 샤드로 구성됩니다.</p>
 * 
 * <p>리뷰 작업의 상태 흐름:</p>
 * <ul>
 *   <li>QUEUED: 작업이 큐에 등록됨</li>
 *   <li>RUNNING: 작업이 실행 중</li>
 *   <li>SUCCEEDED: 모든 샤드가 성공적으로 처리됨</li>
 *   <li>PARTIAL: 일부 샤드는 성공했지만 일부는 실패함</li>
 *   <li>FAILED: 작업이 완전히 실패함</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "pr_review_job", 
       uniqueConstraints = @UniqueConstraint(columnNames = {"pull_request_id", "head_sha"}))
public class PrReviewJob extends BaseEntity {

    /**
     * 리뷰 작업의 상태를 나타내는 열거형
     */
    public enum Status {
        /** 작업이 큐에 등록됨 */
        QUEUED,
        /** 작업이 현재 실행 중 */
        RUNNING,
        /** 작업이 성공적으로 완료됨 */
        SUCCEEDED,
        /** 작업이 일부 실패와 함께 완료됨 */
        PARTIAL,
        /** 작업이 완전히 실패함 */
        FAILED
    }

    /** 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "prReviewJob", cascade = CascadeType.ALL)
    private List<LlmTask> llmTasks = new ArrayList<>();

    /** 이 작업이 속한 Pull Request */
    // FetchType.LAZY에서 EAGER로 변경 - PrReviewJob 조회 시 PullRequest도 함께 조회 필요
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    /** 이 작업과 연결된 채팅방 */
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_thread_id")
    private ChatRoom chatRoom;

    /** PR의 HEAD SHA */
    @Column(name = "head_sha", nullable = false)
    private String headSha;

    /** 작업 상태 */
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    private Status status;

    /** 작업 시작 시간 */
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    /** 작업 완료 시간 */
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    /** 오류 메시지 */
    @Column(name = "error_message", length = 1000)
    private String errorMessage;

    /**
     * 작업 상태를 업데이트하고 필요시 시간 정보를 갱신
     * 
     * @param newStatus 새로운 상태
     */
    public void updateStatus(Status newStatus) {
        this.status = newStatus;
        
        if (newStatus == Status.RUNNING && this.startedAt == null) {
            this.startedAt = LocalDateTime.now();
        }
        
        if ((newStatus == Status.SUCCEEDED || newStatus == Status.PARTIAL || newStatus == Status.FAILED) 
                && this.completedAt == null) {
            this.completedAt = LocalDateTime.now();
        }
    }

    /**
     * 오류 메시지 기록
     * 
     * @param message 오류 메시지
     */
    public void recordError(String message) {
        this.errorMessage = message;
    }

    /**
     * PR 번호를 PullRequest를 통해 조회
     */
    public Integer getPrNumber() {
        return pullRequest != null ? pullRequest.getPrNumber() : null;
    }

    /**
     * ProjectRepo를 PullRequest를 통해 조회
     */
    public ProjectRepo getProjectRepo() {
        return pullRequest != null ? pullRequest.getProjectRepo() : null;
    }
}