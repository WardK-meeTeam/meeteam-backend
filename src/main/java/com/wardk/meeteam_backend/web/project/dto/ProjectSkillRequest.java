package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ProjectSkillRequest {

    private String name;


    public ProjectSkillRequest(ProjectSkill projectSkill) {
        this.name = projectSkill.getSkill().getSkillName();
    }
}
