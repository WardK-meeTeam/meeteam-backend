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

    public static ProjectResponse responseDto(Project project) {

        return ProjectResponse.builder()
                .name(project.getName())
                .description(project.getDescription())
                .platformCategory(project.getPlatformCategory())
                .projectCategory(project.getProjectCategory())
                .imageUrl(project.getImageUrl())
                .offlineRequired(project.isOfflineRequired())
                .startDate(project.getStartDate())
                .endDate(project.getEndDate())
                .projectMembers(
                        project.getMembers().stream()
                                .map(member -> ProjectMemberListResponse.responseDto(
                                        member.getMember().getId(),
                                        member.getMember().getRealName(),
                                        member.getMember().getStoreFileName(),
                                        project.getCreator().getId().equals(member.getId())
                                ))
                                .toList()
                )
                .skills(
                        project.getProjectSkills().stream()
                                .map(ps -> ps.getSkill().getSkillName())
                                .toList()
                )
                .recruitments(
                        project.getRecruitments().stream()
                                .map(r -> RecruitmentDto.responseDto(
                                        r.getSubCategory().getName(),
                                        r.getRecruitmentCount(),
                                        r.getCurrentCount()
                                ))
                                .toList()
                )
                .build();
    }
}
