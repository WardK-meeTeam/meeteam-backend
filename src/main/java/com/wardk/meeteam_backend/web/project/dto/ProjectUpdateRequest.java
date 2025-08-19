package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ProjectUpdateRequest {

    @Schema(description = "프로젝트 제목", example = "test")
    private String name;

    @Size(max = 5000, message = "설명은 최대 5,000자까지 입력 가능합니다.")
    @Schema(description = "프로젝트 설명", example = "test description")
    private String description;

    @Schema(description = "프로젝트 카테고리", example = "ENVIRONMENT")
    private ProjectCategory projectCategory;

    @Schema(description = "플랫폼 카테고리", example = "IOS")
    private PlatformCategory platformCategory;

    private String imageUrl;

    @Schema(description = "오프라인 필수 여부", example = "true")
    private boolean offlineRequired;

    @Schema(description = "프로젝트 상태", example = "PLANNING")
    private ProjectStatus status;

    @Schema(description = "프로젝트 시작 날짜", example = "2025-08-19")
    private LocalDate startDate;

    @Schema(description = "프로젝트 종료 날짜", example = "2025-09-19")
    private LocalDate endDate;

    @Schema(
            description = "모집분야 리스트",
            example = "[{\"subCategory\": \"웹서버\", \"recruitmentCount\": 2}, {\"subCategory\": \"UI/UX디자인\", \"recruitmentCount\": 1}]"
    )
    private List<ProjectRecruitDto> recruitments;

    @Schema(
            description = "기술 스택 리스트",
            example = "[{\"skillName\": \"Java\"}, {\"skillName\": \"Spring\"}]"
    )
    private List<ProjectSkillDto> skills;
}
