package com.wardk.meeteam_backend.web.project.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ProjectSkillDto {

    @NotNull
    private String skillName;
}
