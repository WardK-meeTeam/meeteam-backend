package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProjectPostResponseDto {

    private Long id;

    private String title;

    private LocalDateTime createdAt;

    public ProjectPostResponseDto(Project project) {
        this.id = project.getId();
        this.title = project.getName();
        this.createdAt = project.getCreatedAt();
    }


    // or static factory
    public static ProjectPostResponseDto from(Project project) {
        return new ProjectPostResponseDto(project);
    }

}
