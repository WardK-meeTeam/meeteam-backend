package com.wardk.meeteam_backend.domain.codereview.entity;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * PR 리뷰 발견 항목 엔티티
 * 
 * <p>코드 리뷰 과정에서 발견된 개별 항목을 저장합니다. 각 항목은 심각도, 파일 경로, 라인 범위,
 * 제목, 메시지, 제안 패치 등의 정보를 포함합니다.</p>
 * 
 * <p>발견 항목의 상태 흐름:</p>
 * <ul>
 *   <li>OPEN: 초기 상태, 발견되었지만 아직 처리되지 않음</li>
 *   <li>RESOLVED: 개발자가 해결함</li>
 *   <li>IGNORED: 개발자가 무시하기로 결정함</li>
 * </ul>
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "pr_review_finding")
public class PrReviewFinding extends BaseEntity {

    /**
     * 발견 항목의 심각도를 나타내는 열거형
     */
    public enum Severity {
        /** 정보 제공 수준 */
        NOTICE,
        /** 잠재적 문제 */
        WARNING,
        /** 심각한 문제 */
        FAILURE
    }

    /**
     * 발견 항목의 상태를 나타내는 열거형
     */
    public enum Status {
        /** 초기 상태 */
        OPEN,
        /** 해결됨 */
        RESOLVED,
        /** 무시됨 */
        IGNORED
    }

    /** 고유 식별자 */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** 이 발견 항목이 속한 리뷰 작업 */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pr_review_job_id", nullable = false)
    private PrReviewJob prReviewJob;

    /** 이 발견 항목이 속한 Pull Request */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pull_request_id", nullable = false)
    private PullRequest pullRequest;

    /** 발견 항목이 위치한 파일 경로 */
    @Column(name = "file_path", nullable = false)
    private String filePath;

    /** 발견 항목의 시작 라인 */
    @Column(name = "start_line")
    private Integer startLine;

    /** 발견 항목의 끝 라인 */
    @Column(name = "end_line")
    private Integer endLine;

    /** 심각도 */
    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private Severity severity;

    /** 제목 (한 줄 요약) */
    @Column(nullable = true, length = 255)
    private String title;

    /** 상세 메시지 */
    @Column(nullable = true, columnDefinition = "TEXT")
    private String message;

    /** 제안된 패치 내용 */
    @Column(name = "suggested_patch", columnDefinition = "TEXT")
    private String suggestedPatch;

    /** 이 발견 항목이 생성된 샤드 ID */
    @Column(name = "shard_id", length = 64)
    private String shardId;

    /** 상태 */
    @Column(nullable = true)
    @Enumerated(EnumType.STRING)
    private Status status;

    /** 정렬 및 우선순위 지정을 위한 점수 */
    @Column
    private Integer score;

    // 많은양의 텍스트를 저장할 수 있도록 함
    @Column(name = "chat_response", columnDefinition = "LONGTEXT")
    private String chatResponse;

    /**
     * 엔티티 저장 전 UUID 생성
     */
    @PrePersist
    public void prePersist() {
        if (this.status == null) {
            this.status = Status.OPEN;
        }
    }

    /**
     * 상태 업데이트
     * 
     * @param newStatus 새로운 상태
     */
    public void updateStatus(Status newStatus) {
        this.status = newStatus;
    }

}
