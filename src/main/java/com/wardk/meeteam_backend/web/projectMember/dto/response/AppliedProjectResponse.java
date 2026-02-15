package com.wardk.meeteam_backend.web.projectmember.dto.response;

import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectApplication;
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
    private JobPosition jobPosition;

    public static AppliedProjectResponse responseDto(ProjectApplication application) {

        return AppliedProjectResponse.builder()
                .applicationId(application.getId())
                .projectId(application.getProject().getId())
                .projectName(application.getProject().getName())
                .createdAt(application.getCreatedAt())
                .jobPosition(application.getJobPosition())
                .build();
    }
}
