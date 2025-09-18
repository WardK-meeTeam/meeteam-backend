package com.wardk.meeteam_backend.domain.pr.entity;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(
        name = "project_repo",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_project_repo",
                        columnNames = {"repo_full_name"}
                )
        }
)
public class ProjectRepo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @Column(name = "repo_full_name",nullable = false)
    private String repoFullName;

    private Long installationId;

    @Builder
    public ProjectRepo(Project project, String repoFullName, Long installationId) {
        this.project = project;
        this.repoFullName = repoFullName;
        this.installationId = installationId;
    }

    public static ProjectRepo create(Project project, String repoFullName, Long installationId) {
        return ProjectRepo.builder()
                .project(project)
                .repoFullName(repoFullName)
                .installationId(installationId)
                .build();
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
