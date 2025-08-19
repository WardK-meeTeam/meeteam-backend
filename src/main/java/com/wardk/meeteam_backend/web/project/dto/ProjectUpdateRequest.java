package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import org.springframework.validation.annotation.Validated;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ProjectUpdateRequest {

    @Schema(description = "프로젝트 제목", example = "test")
    @NotBlank(message = "제목은 필수입니다.")
    private String name;

    @Schema(description = "프로젝트 설명", example = "test description")
    @Size(max = 5000, message = "설명은 최대 5,000자까지 입력 가능합니다.")
    private String description;

    @Schema(description = "프로젝트 카테고리", example = "ENVIRONMENT")
    @NotNull(message = "카테고리를 선택해주세요.")
    private ProjectCategory projectCategory;

    @Schema(description = "플랫폼 카테고리", example = "IOS")
    @NotNull(message = "플랫폼을 선택해주세요.")
    private PlatformCategory platformCategory;

    @Schema(description = "오프라인 필수 여부", example = "true")
    @NotNull(message = "오프라인 필수 여부를 선택해주세요.")
    private Boolean offlineRequired;

    @Schema(description = "프로젝트 상태", example = "PLANNING")
    @NotNull(message = "프로젝트 상태를 선택해주세요.")
    private ProjectStatus status;

    @Schema(description = "프로젝트 시작 날짜", example = "2025-08-19")
    @NotNull(message = "프로젝트 시작 날짜를 입력해주세요.")
    private LocalDate startDate;

    @Schema(description = "프로젝트 종료 날짜", example = "2025-09-19")
    @NotNull(message = "프로젝트 종료 날짜를 입력해주세요.")
    private LocalDate endDate;

    @Schema(
            description = "모집분야 리스트",
            example = "[{\"subCategory\": \"웹서버\", \"recruitmentCount\": 2}, {\"subCategory\": \"UI/UX디자인\", \"recruitmentCount\": 1}]"
    )
    @Valid
    private List<ProjectRecruitDto> recruitments;

    @Schema(
            description = "기술 스택 리스트",
            example = "[{\"skillName\": \"Java\"}, {\"skillName\": \"Spring\"}]"
    )
    @Valid
    private List<ProjectSkillDto> skills;
}
