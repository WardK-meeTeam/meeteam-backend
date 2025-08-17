package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectMemberListResponse;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ProjectResponse {

    private String name;
    private String description;
    private PlatformCategory platformCategory;
    private ProjectCategory projectCategory;
    private String imageUrl;
    private boolean offlineRequired;
    private LocalDate startDate; // 게시일이랑 프로젝트 시작일이랑 똑같은 건가?
    private LocalDate endDate;
    private List<ProjectMemberListResponse> projectMembers;
    private List<String> skills;
    private List<RecruitmentDto> recruitments;

    public static ProjectResponse responseDto(String name, String description, PlatformCategory platformCategory,
                                              ProjectCategory projectCategory, String imageUrl, boolean offlineRequired,
                                              LocalDate startDate, LocalDate endDate,
                                              List<ProjectMemberListResponse> projectMembers,
                                              List<String> skills,
                                              List<RecruitmentDto> recruitments) {

        return ProjectResponse.builder()
                .name(name)
                .description(description)
                .platformCategory(platformCategory)
                .projectCategory(projectCategory)
                .imageUrl(imageUrl)
                .offlineRequired(offlineRequired)
                .startDate(startDate)
                .endDate(endDate)
                .projectMembers(projectMembers)
                .skills(skills)
                .recruitments(recruitments)
                .build();
    }
}
