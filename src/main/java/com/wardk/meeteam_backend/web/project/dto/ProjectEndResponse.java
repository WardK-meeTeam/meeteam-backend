package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class ProjectEndResponse {

    private Long projectId;
    private ProjectStatus status;

    public static ProjectEndResponse responseDto(Long projectId, ProjectStatus status) {
        return ProjectEndResponse.builder()
                .projectId(projectId)
                .status(status)
                .build();
    }
}
