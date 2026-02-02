package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectMemberListResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectConditionRequest {



    private Long projectId;
    private ProjectCategory projectCategory;
    private PlatformCategory platformCategory;
    private List<String> skills; // 프로젝트 스킬
    private String projectName; // 프로젝트 이름
    private String creatorName; // 팀장 이름
    private LocalDate startDate; // 프로젝트 생성일
    private LocalDate endDate; // 프로젝트 마감일(목표기간)
    private String projectImageUrl; // 프로젝트 이미지
    private Long currentCount; // 프로젝트 참여한 인원
    private Long recruitmentCount; // 프로젝트 총 모집 인원
    // 좋아요 관련 필드
    private boolean isLiked; // 좋아요 여부 (비로그인, 로그인 분기)
    private int likeCount; // 좋아요 개수

    // 프로젝트 인원 id, name, imageUrl(참가자 + 팀장)
    List<ProjectMemberListResponse> projectMembers;




    public ProjectConditionRequest(Project project, boolean isLiked,
                                   Long currentCount, Long recruitmentCount,
                                   List<ProjectMemberListResponse> projectMembers
    ) {
        this.projectId = project.getId();
        this.projectCategory = project.getProjectCategory();
        this.platformCategory = project.getPlatformCategory();
        this.skills = project.getProjectSkills()
                .stream()
                .map(projectSkill -> projectSkill.getSkill().getSkillName())
                .toList();
        this.projectName = project.getName();
        this.creatorName = project.getCreator().getRealName();
        this.startDate = LocalDate.from(project.getCreatedAt());
        this.endDate = LocalDate.from(project.getEndDate());
        this.projectImageUrl = project.getImageUrl();
        this.isLiked = isLiked;
        this.likeCount = project.getLikeCount();
        this.currentCount = currentCount;
        this.recruitmentCount = recruitmentCount;
        this.projectMembers = projectMembers;
    }

}
