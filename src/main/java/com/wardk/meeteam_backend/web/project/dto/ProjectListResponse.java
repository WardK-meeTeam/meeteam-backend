package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
public class ProjectListResponse {

    private Long projectId;
    private String name;
    private String imageUrl;
    private String creatorName;
    private List<String> skills;
    private LocalDate startDate;

    public static ProjectListResponse responseDto(Long projectId, String name, String imageUrl, String creatorName, List<String> skills, LocalDate startDate) {
        return ProjectListResponse.builder()
                .projectId(projectId)
                .name(name)
                .imageUrl(imageUrl)
                .creatorName(creatorName)
                .skills(skills)
                .startDate(startDate)
                .build();
    }
}
