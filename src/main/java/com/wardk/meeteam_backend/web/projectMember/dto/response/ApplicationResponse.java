package com.wardk.meeteam_backend.web.projectmember.dto.response;

import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationResponse {

    private Long id;
    private Long projectId;
    private Long applicantId;

    public static ApplicationResponse responseDto(ProjectApplication application) {
        return ApplicationResponse.builder()
                .id(application.getId())
                .projectId(application.getProject().getId())
                .applicantId(application.getApplicant().getId())
                .build();
    }
}
