package com.wardk.meeteam_backend.web.project.dto;


import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springdoc.core.annotations.ParameterObject;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@ParameterObject
public class ProjectPostRequestDto {


    /** 프로젝트 제목 */
    @NotBlank(message = "제목은 필수입니다.")
    private String projectName;

    @NotNull(message = "카테고리를 선택해주세요.")
    private ProjectCategory projectCategory;


    @NotNull(message = "플랫폼을 선택해주세요.")
    private PlatformCategory platformCategory;


    /** 오프라인 필수 여부 */
    private Boolean offlineRequired;


    // 소분류 모집분야
    private List<SubCategoryDto> subCategories = new ArrayList<>();

    // 프로젝트 기술스택들
    private List<ProjectSkillDto> projectSkills = new ArrayList<>();


    /** 설명(길이 긴 텍스트) */
    @Size(max = 5000, message = "설명은 최대 5,000자까지 입력 가능합니다.")
    private String description;

    @Schema(description = "모집 마감일")
    private LocalDate deadline;
}
