package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.PlatformCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectCategory;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
public class ProjectUpdateRequest {

    private String name;
    private String description;
    private ProjectCategory projectCategory;
    private PlatformCategory platformCategory;
    private String imageUrl;
    private Boolean offlineRequired;
    private ProjectStatus status;
    private LocalDate startDate;
    private LocalDate endDate;
    private List<ProjectRecruitDto> recruitments;
    private List<ProjectSkillDto> skills;
}
