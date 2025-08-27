package com.wardk.meeteam_backend.web.mainpage.dto;

import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class ProjectCardMainResponse {
    private PlatformCategory platformCategory;

    private List<ProjectStackRequest> projectSkills;

    private String projectName;

    private String creatorName;

    private LocalDate localDate;
}
