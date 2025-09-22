package com.wardk.meeteam_backend.web.member.dto;

import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.projectMember.entity.ApplicationStatus;
import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDate;

@AllArgsConstructor
@Data
public class ProjectResponse {

    private Long projectId;

    private LocalDate localDate;

    private String title;

    private String imageUrl;

    private ProjectStatus status;

}
