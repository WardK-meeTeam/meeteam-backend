package com.wardk.meeteam_backend.domain.pr.entity;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

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

    @Column(name = "description")
    private String description;

    @Column(name = "star_count")
	private Long starCount;

    @Column(name = "watcher_count")
    private Long watcherCount;

    @Column(name = "pushed_at")
    private LocalDateTime pushedAt;

    @Column(name = "language")
    private String language;

    @Builder
    public ProjectRepo(Project project, String repoFullName, Long installationId, String description, Long starCount, Long watcherCount, LocalDateTime pushedAt, String language) {
        this.project = project;
        this.repoFullName = repoFullName;
        this.installationId = installationId;
        this.description = description;
        this.starCount = starCount;
        this.watcherCount = watcherCount;
        this.pushedAt = pushedAt;
        this.language = language;
    }

    public static ProjectRepo create(
            Project project,
            String repoFullName,
            Long installationId,
            String description,
            Long starCount,
            Long watcherCount,
            LocalDateTime pushedAt,
            String language) {
        return ProjectRepo.builder()
                .project(project)
                .repoFullName(repoFullName)
                .installationId(installationId)
                .description(description)
                .starCount(starCount)
                .watcherCount(watcherCount)
                .pushedAt(pushedAt)
                .language(language)
                .build();
    }

    public void setProject(Project project) {
        this.project = project;
    }
}
