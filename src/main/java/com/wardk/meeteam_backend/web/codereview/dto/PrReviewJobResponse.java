package com.wardk.meeteam_backend.web.codereview.dto;

import com.wardk.meeteam_backend.domain.codereview.entity.PrReviewJob;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class PrReviewJobResponse {

    private Long id;
    private Integer prNumber;
    private String repoFullName;
    private String headSha;
    private String status;
    private Long chatRoomId;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static PrReviewJobResponse from(PrReviewJob reviewJob) {
        return PrReviewJobResponse.builder()
                .id(reviewJob.getId())
                .prNumber(reviewJob.getPrNumber())
                .repoFullName(reviewJob.getProjectRepo().getRepoFullName())
                .headSha(reviewJob.getHeadSha())
                .status(reviewJob.getStatus().name())
                .chatRoomId(reviewJob.getChatRoom() != null ? reviewJob.getChatRoom().getId() : null)
                .startedAt(reviewJob.getStartedAt())
                .completedAt(reviewJob.getCompletedAt())
                .errorMessage(reviewJob.getErrorMessage())
                .createdAt(reviewJob.getCreatedAt())
                .updatedAt(reviewJob.getEditedAt())
                .build();
    }
}
