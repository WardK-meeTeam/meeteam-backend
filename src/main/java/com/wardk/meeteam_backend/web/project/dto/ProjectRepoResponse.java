package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectRepoResponse {

    private Long id;

    private String repoFullName;

    public static ProjectRepoResponse responseDto(ProjectRepo projectRepo) {
        return ProjectRepoResponse.builder()
                .id(projectRepo.getId())
                .repoFullName(projectRepo.getRepoFullName())
                .build();
    }
}
