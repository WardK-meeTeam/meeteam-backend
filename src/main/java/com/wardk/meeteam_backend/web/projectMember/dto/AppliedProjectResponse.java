package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AppliedProjectResponse {

    private Long applicationId;
    private Long projectId;
    private String projectName;
    private LocalDateTime createdAt;
    private String subCategoryName;

    public static AppliedProjectResponse responseDto(ProjectMemberApplication application) {

        return AppliedProjectResponse.builder()
                .applicationId(application.getId())
                .projectId(application.getProject().getId())
                .projectName(application.getProject().getName())
                .createdAt(application.getCreatedAt())
                .subCategoryName(application.getSubCategory().getName())
                .build();
    }
}
