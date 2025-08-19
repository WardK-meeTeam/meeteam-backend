package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectApplicationListResponse {

    private Long applicationId;
    private Long applicantId;
    private String applicantName;
    private String subCategoryName;

    public static ProjectApplicationListResponse responseDto(ProjectMemberApplication application) {
        return ProjectApplicationListResponse.builder()
                .applicationId(application.getId())
                .applicantId(application.getApplicant().getId())
                .applicantName(application.getApplicant().getRealName())
                .subCategoryName(application.getSubCategory().getName())
                .build();
    }
}
