package com.wardk.meeteam_backend.domain.project.service.dto;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.RecruitmentDeadlineType;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectPostRequest;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectRecruitRequest;

import java.time.LocalDate;
import java.util.List;

public record ProjectPostCommand(
        String projectName,
        String githubRepositoryUrl,
        String communicationChannelUrl,
        ProjectCategory projectCategory,
        String description,
        PlatformCategory platformCategory,
        JobPosition jobPosition,
        List<ProjectRecruitRequest> recruitments,
        List<String> skills,
        RecruitmentDeadlineType recruitmentDeadlineType,
        LocalDate endDate
) {
    public static ProjectPostCommand from(ProjectPostRequest projectPostRequest) {
        return new ProjectPostCommand(
                projectPostRequest.getProjectName(),
                projectPostRequest.getGithubRepositoryUrl(),
                projectPostRequest.getCommunicationChannelUrl(),
                projectPostRequest.getProjectCategory(),
                projectPostRequest.getDescription(),
                projectPostRequest.getPlatformCategory(),
                projectPostRequest.getJobPosition(),
                projectPostRequest.getRecruitments(),
                projectPostRequest.getSkills(),
                projectPostRequest.getRecruitmentDeadlineType(),
                projectPostRequest.getEndDate()
        );
    }
}
