package com.wardk.meeteam_backend.web.project.dto.request;


import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class ProjectPostRequest {


    /** 프로젝트 제목 */
    @NotNull(message = "제목은 필수입니다.")
    @Schema(description = "프로젝트 제목", example = "test")
    private String projectName;

    /** 설명(길이 긴 텍스트) */
    @Size(max = 5000, message = "설명은 최대 5,000자까지 입력 가능합니다.")
    @Schema(description = "프로젝트 설명", example = "test description")
    private String description;

    @NotNull(message = "카테고리를 선택해주세요.")
    @Schema(description = "프로젝트 카테고리", example = "ENVIRONMENT")
    private ProjectCategory projectCategory;

    @NotNull(message = "플랫폼을 선택해주세요.")
    @Schema(description = "플랫폼 카테고리", example = "IOS")
    private PlatformCategory platformCategory;

    /** 오프라인 필수 여부 */
    @NotNull(message = "오프라인 필수 여부를 선택해주세요.")
    @Schema(description = "오프라인 필수 여부", example = "true")
    private Boolean offlineRequired;

    @NotNull(message = "프로젝트 생성자의 직무 포지션을 입력해주세요")
    @Schema(description = "생성자의 직무 포지션", example = "WEB_FRONTEND")
    private JobPosition jobPosition;

    // 모집분야
    @NotNull(message = "모집 분야는 필수입니다.")
    @Valid
    @Size(min = 1, message = "최소 한 개 이상의 모집 분야를 입력해주세요.")
    @Schema(
            description = "모집분야 리스트",
            example = "[{\"jobPosition\": \"WEB_SERVER\", \"recruitmentCount\": 2}, {\"jobPosition\": \"UI_UX_DESIGN\", \"recruitmentCount\": 1}]"
    )
    private List<ProjectRecruitRequest> recruitments = new ArrayList<>();

    // 프로젝트 기술스택들
    @NotNull(message = "기술 스택은 필수입니다.")
    @Size(min = 1, message = "최소 한 개 이상의 기술 스택을 입력 해주세요.")
    @Schema(
            description = "기술 스택 리스트",
            example = "[\"Java\", \"Spring\"]"
    )
    private List<String> skills = new ArrayList<>();

    @NotNull(message = "프로젝트 마감일을 입력 해주세요.")
    @Schema(description = "프로젝트 마감일")
    private LocalDate endDate;
}
