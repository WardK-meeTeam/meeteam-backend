package com.wardk.meeteam_backend.web.project.dto;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.querydsl.core.annotations.QueryProjection;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonPropertyOrder(
        {
                "projectId","projectCategory", "platformCategory", "projectSkills"
                ,"projectName", "creatorName", "localDate"
        }
)
public class ProjectSearchResponse {


    private Long projectId;

    private ProjectCategory projectCategory;

    private PlatformCategory platformCategory;

    private List<ProjectSkillRequest> projectSkills;

    private String projectName;

    private String creatorName;

    private LocalDate localDate;

    public void settingSkills(Project project) {
        projectSkills = project.getProjectSkills()
                .stream()
                .map(projectSkill -> new ProjectSkillRequest(projectSkill))
                .toList();
    }

    @QueryProjection
    public ProjectSearchResponse(
            Long projectId,
            PlatformCategory platformCategory,
                                 String projectName,
                                 String creatorName,
                                 LocalDateTime localDateTime,
                                ProjectCategory projectCategory
    ) {
        this.projectId = projectId;
        this.projectCategory = projectCategory;
        this.platformCategory = platformCategory;
        this.projectName = projectName;
        this.creatorName = creatorName;
        this.localDate = localDateTime.toLocalDate();
    }
}
