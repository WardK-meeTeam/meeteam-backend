package com.wardk.meeteam_backend.web.projectmember.dto.response;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.projectmember.entity.ProjectApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectApplicationListResponse {

    private Long applicationId;
    private Long applicantId;
    private String applicantName;
    private JobPosition jobPosition;

    public static ProjectApplicationListResponse responseDto(ProjectApplication application) {
        return ProjectApplicationListResponse.builder()
                .applicationId(application.getId())
                .applicantId(application.getApplicant().getId())
                .applicantName(application.getApplicant().getRealName())
                .jobPosition(application.getJobPosition())
                .build();
    }
}
