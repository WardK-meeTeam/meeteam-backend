package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
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
    private List<String> projectSkills;
    private String projectName;
    private String creatorName;
    private LocalDate localDate;
    // 좋아요 관련 필드
    private boolean isLiked;
    private int likeCount;




    public ProjectConditionRequest(Project project, boolean isLiked) {
        this.projectId = project.getId();
        this.projectCategory = project.getProjectCategory();
        this.platformCategory = project.getPlatformCategory();
        this.projectSkills = project.getProjectSkills()
                .stream()
                .map(projectSkill -> projectSkill.getSkill().getSkillName())
                .toList();
        this.projectName = project.getName();
        this.creatorName = project.getCreator().getRealName();
        this.localDate = LocalDate.from(project.getCreatedAt());
        this.isLiked = isLiked;
        this.likeCount = project.getLikeCount();
    }

}
