package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ProjectRepoResponse {

    private Long id;

    private String repoFullName;
    private String description;
    private Long starCount;
    private Long watcherCount;
    private LocalDateTime pushedAt;
    private String language;


    public static ProjectRepoResponse responseDto(ProjectRepo projectRepo) {
        return ProjectRepoResponse.builder()
                .id(projectRepo.getId())
                .repoFullName(projectRepo.getRepoFullName())
                .description(projectRepo.getDescription())
                .starCount(projectRepo.getStarCount())
                .watcherCount(projectRepo.getWatcherCount())
                .pushedAt(projectRepo.getPushedAt())
                .language(projectRepo.getLanguage())
                .build();
    }
}
