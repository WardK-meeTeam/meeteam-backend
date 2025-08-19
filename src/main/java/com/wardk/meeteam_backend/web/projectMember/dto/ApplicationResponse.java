package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationResponse {

    private Long id;
    private Long projectId;
    private Long applicantId;

    public static ApplicationResponse responseDto(ProjectMemberApplication application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .projectId(application.getProject().getId())
                .applicantId(application.getApplicant().getId())
                .build();
    }
}
