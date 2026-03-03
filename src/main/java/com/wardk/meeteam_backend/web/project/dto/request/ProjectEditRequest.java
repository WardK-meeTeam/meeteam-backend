package com.wardk.meeteam_backend.web.project.dto.request;

import com.wardk.meeteam_backend.domain.job.entity.JobPositionCode;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.service.dto.ProjectEditCommand;
import com.wardk.meeteam_backend.domain.project.service.dto.RecruitmentEditCommand;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

import static com.wardk.meeteam_backend.global.constant.PatternConstants.COMMUNICATION_CHANNEL_URL_PATTERN;
import static com.wardk.meeteam_backend.global.constant.PatternConstants.GITHUB_REPOSITORY_URL_PATTERN;

/**
 * 프로젝트 수정 요청 DTO.
 */
public record ProjectEditRequest(
        @NotBlank(message = "제목은 필수입니다.")
        @Schema(description = "프로젝트 제목", example = "MeeTeam 프로젝트")
        String name,

        @Size(max = 5000, message = "설명은 최대 5,000자까지 입력 가능합니다.")
        @Schema(description = "프로젝트 설명", example = "개발자 팀 매칭 플랫폼입니다.")
        String description,

        @NotNull(message = "카테고리를 선택해주세요.")
        @Schema(description = "프로젝트 카테고리", example = "IT_SERVICE")
        ProjectCategory projectCategory,

        @NotNull(message = "출시 플랫폼을 선택해주세요.")
        @Schema(description = "출시 플랫폼 카테고리", example = "WEB")
        PlatformCategory platformCategory,

        @Schema(description = "Github 레포 주소", example = "https://github.com/username/repository")
        @Pattern(regexp = GITHUB_REPOSITORY_URL_PATTERN)
        String githubRepositoryUrl,

        @Schema(description = "소통 채널 링크", example = "https://discord.gg/abc123")
        @Pattern(regexp = COMMUNICATION_CHANNEL_URL_PATTERN)
        String communicationChannelUrl,

        @NotNull(message = "프로젝트 마감일을 입력해주세요.")
        @Schema(description = "프로젝트 마감일", example = "2025-12-31")
        LocalDate endDate,

        @NotNull(message = "리더 포지션 코드를 입력해주세요.")
        @Schema(description = "리더 직무 포지션 코드", example = "JAVA_SPRING")
        JobPositionCode leaderJobPositionCode,

        @NotEmpty(message = "최소 한 개 이상의 모집 분야를 입력해주세요.")
        @Valid
        @Schema(description = "모집분야 리스트")
        List<RecruitmentEditRequest> recruitments,

        @Schema(description = "대기 지원자가 있는 포지션 삭제 확인 여부", example = "false")
        Boolean confirmDeletePositionsWithPendingApplicants
) {
    /**
     * Request DTO를 도메인 Command로 변환합니다.
     */
    public ProjectEditCommand toCommand() {
        List<RecruitmentEditCommand> recruitmentCommands = this.recruitments.stream()
                .map(RecruitmentEditRequest::toCommand)
                .toList();

        return new ProjectEditCommand(
                this.name,
                this.description,
                this.projectCategory,
                this.platformCategory,
                this.githubRepositoryUrl,
                this.communicationChannelUrl,
                this.endDate,
                this.leaderJobPositionCode,
                recruitmentCommands,
                this.confirmDeletePositionsWithPendingApplicants != null && this.confirmDeletePositionsWithPendingApplicants
        );
    }
}
