package com.wardk.meeteam_backend.web.application.dto.response;

import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

/**
 * 프로젝트 지원자 목록 응답 DTO.
 * 프로젝트 리더가 지원자 목록을 조회할 때 사용됩니다.
 */
@Schema(description = "프로젝트 지원자 목록 응답")
public record ProjectApplicationListResponse(
        @Schema(description = "지원서 ID (상세 보기, 승인/거절에 사용)")
        Long applicationId,

        @Schema(description = "지원자 ID")
        Long applicantId,

        @Schema(description = "지원자 이름")
        String applicantName,

        @Schema(description = "지원자 프로필 이미지 URL")
        String profileImageUrl,

        @Schema(description = "지원자 이메일")
        String applicantEmail,

        @Schema(description = "직군 ID")
        Long jobFieldId,

        @Schema(description = "직군명")
        String jobFieldName,

        @Schema(description = "포지션 ID")
        Long jobPositionId,

        @Schema(description = "포지션명")
        String jobPositionName,

        @Schema(description = "지원 사유 및 자기소개")
        String motivation,

        @Schema(description = "지원 일시")
        LocalDateTime appliedAt
) {
    public static ProjectApplicationListResponse from(ProjectApplication application) {
        return new ProjectApplicationListResponse(
                application.getId(),
                application.getApplicant().getId(),
                application.getApplicant().getRealName(),
                application.getApplicant().getStoreFileName(),
                application.getApplicant().getEmail(),
                application.getJobPosition().getJobField().getId(),
                application.getJobPosition().getJobField().getName(),
                application.getJobPosition().getId(),
                application.getJobPosition().getName(),
                application.getMotivation(),
                application.getCreatedAt()
        );
    }
}
