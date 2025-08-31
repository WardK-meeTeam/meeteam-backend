package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@Builder
public class MyProjectResponse {

    private Long projectId;
    private String projectName;
    private ProjectStatus projectStatus;
    private LocalDate startDate;
    private LocalDate endDate;
    private String subCategoryName;

    public static MyProjectResponse responseDto(ProjectMember pm) {
        return MyProjectResponse.builder()
                .projectId(pm.getProject().getId())
                .projectName(pm.getProject().getName())
                .projectStatus(pm.getProject().getStatus())
                .startDate(pm.getProject().getStartDate())
                .endDate(pm.getProject().getEndDate())
                .subCategoryName(pm.getSubCategory().getName())
                .build();
    }
}
