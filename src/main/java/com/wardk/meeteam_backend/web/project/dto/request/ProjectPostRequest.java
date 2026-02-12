package com.wardk.meeteam_backend.web.project.dto.request;


import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static com.wardk.meeteam_backend.global.constant.PatternConstants.COMMUNICATION_CHANNEL_URL_PATTERN;
import static com.wardk.meeteam_backend.global.constant.PatternConstants.GITHUB_REPOSITORY_URL_PATTERN;

@Data
public class ProjectPostRequest {

    @NotBlank(message = "제목은 필수입니다.")
    @Schema(description = "프로젝트 제목", example = "test")
    private String projectName;

    @Schema(description = "Github 레포 주소", example = "https://github.com/username/repository")
    @Pattern(regexp = GITHUB_REPOSITORY_URL_PATTERN)
    private String githubRepositoryUrl;

    @Schema(description = "소통 채널 링크 (디코, 슬랙, 카카오톡 오픈 채팅)", example = "https://discord.gg/abc123")
    @Pattern(regexp = COMMUNICATION_CHANNEL_URL_PATTERN)
    private String communicationChannelUrl;

    @NotNull(message = "카테고리를 선택해주세요.")
    @Schema(description = "프로젝트 카테고리", example = "ENVIRONMENT")
    private ProjectCategory projectCategory;

    @Size(max = 5000, message = "설명은 최대 5,000자까지 입력 가능합니다.")
    @Schema(description = "프로젝트 설명", example = "test description")
    private String description;

    @NotNull(message = "출시 플랫폼을 선택해주세요.")
    @Schema(description = "출시 플랫폼 카테고리", example = "WEB")
    private PlatformCategory platformCategory;

    @NotNull(message = "프로젝트 생성자의 직무 포지션을 입력해주세요")
    @Schema(description = "생성자의 직무 포지션", example = "WEB_SERVER")
    private JobPosition jobPosition;

    @NotEmpty(message = "최소 한 개 이상의 모집 분야를 입력해주세요.")
    @Valid
    @Schema(
            description = "모집분야 리스트",
            example = "[{\"jobPosition\": \"WEB_SERVER\", \"recruitmentCount\": 2}, {\"jobPosition\": \"UI_UX_DESIGN\", \"recruitmentCount\": 1}]"
    )
    private List<ProjectRecruitRequest> recruitments = new ArrayList<>();

    @NotEmpty(message = "최소 한 개 이상의 기술 스택을 입력 해주세요.")
    @Schema(
            description = "기술 스택 리스트",
            example = "[\"Java\", \"Spring\"]"
    )
    private List<String> skills = new ArrayList<>();

    @NotNull(message = "프로젝트 마감 방식을 선택해주세요.")
    @Schema(description = "프로젝트 마감 방식", example = "END_DATE, RECRUITMENT_COMPLETED")
    private RecruitmentDeadlineType recruitmentDeadlineType;

    @Schema(description = "프로젝트 마감일")
    private LocalDate endDate;
}
