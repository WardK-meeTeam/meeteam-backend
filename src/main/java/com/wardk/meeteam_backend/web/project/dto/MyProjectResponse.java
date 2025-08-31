package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
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

    public static MyProjectResponse responseDto(Long projectId, String projectName, ProjectStatus projectStatus, LocalDate startDate, LocalDate endDate, String subCategoryName) {
        return MyProjectResponse.builder()
                .projectId(projectId)
                .projectName(projectName)
                .projectStatus(projectStatus)
                .startDate(startDate)
                .endDate(endDate)
                .subCategoryName(subCategoryName)
                .build();
    }
}
