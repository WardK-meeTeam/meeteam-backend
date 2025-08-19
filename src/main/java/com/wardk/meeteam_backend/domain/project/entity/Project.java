package com.wardk.meeteam_backend.domain.project.entity;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import com.wardk.meeteam_backend.domain.review.entity.Review;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Data
@Entity
@NoArgsConstructor
public class Project extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Member creator;

    @Column(name = "project_name")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    private ProjectCategory projectCategory;

    @Enumerated(value = EnumType.STRING)
    private PlatformCategory platformCategory;

    @Column(name = "image_url")
    private String imageUrl;

    @Column(name = "offline_required", nullable = false)
    private Boolean offlineRequired;

    @Enumerated(value = EnumType.STRING)
    private ProjectStatus status;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isDeleted;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectMemberApplication> applications = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectSkill> projectSkills = new ArrayList<>();

    @OneToMany(mappedBy = "project")
    private List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectCategoryApplication> recruitments = new ArrayList<>();

    @Builder
    public Project(Member creator, String name, String description, ProjectCategory projectCategory, PlatformCategory platformCategory,
                   String imageUrl, boolean offlineRequired, ProjectStatus status, LocalDate startDate, LocalDate endDate, Boolean isDeleted) {
        this.creator = creator;
        this.name = name;
        this.description = description;
        this.projectCategory = projectCategory;
        this.platformCategory = platformCategory;
        this.imageUrl = imageUrl;
        this.offlineRequired = offlineRequired;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
        this.isDeleted = isDeleted;
    }

    public static Project createProject(Member creator, String name, String description, ProjectCategory projectCategory, PlatformCategory platformCategory,
                                        String imageUrl, boolean offlineRequired, LocalDate endDate) {
        return Project.builder()
                .creator(creator)
                .name(name)
                .description(description)
                .projectCategory(projectCategory)
                .platformCategory(platformCategory)
                .imageUrl(imageUrl)
                .offlineRequired(offlineRequired)
                .status(ProjectStatus.PLANNING)
                .startDate(LocalDate.now())
                .endDate(endDate)
                .isDeleted(false)
                .build();
    }

    public void joinMember(ProjectMember projectMember) {
        members.add(projectMember);
        projectMember.assignProject(this);
    }

    public void addRecruitment(ProjectCategoryApplication recruitment) {
        this.recruitments.add(recruitment);
        recruitment.assignProject(this);
    }

    public void addProjectSkill(ProjectSkill projectSkill) {
        this.projectSkills.add(projectSkill);
        projectSkill.assignProject(this);
    }

    public void updateProject(String name, String description, ProjectCategory projectCategory, PlatformCategory platformCategory,
                              String imageUrl, Boolean offlineRequired, ProjectStatus status, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.description = description;
        this.projectCategory = projectCategory;
        this.platformCategory = platformCategory;
        this.imageUrl = imageUrl;
        this.offlineRequired = offlineRequired;
        this.status = status;
        this.startDate = startDate;
        this.endDate = endDate;
    }

    public void updateRecruitments(List<ProjectCategoryApplication> recruitments) {
        Map<Long, ProjectCategoryApplication> current = this.recruitments.stream()
                .collect(Collectors.toMap(r -> r.getSubCategory().getId(), r -> r));

        this.recruitments.clear();

        for (ProjectCategoryApplication pca : recruitments) {
            ProjectCategoryApplication existing = current.get(pca.getSubCategory().getId());

            if (existing != null) { // 기존에 존재하면
                pca.updateCurrentCount(existing.getCurrentCount());
                this.addRecruitment(pca);
            } else {
                this.addRecruitment(ProjectCategoryApplication.createProjectCategoryApplication(pca.getSubCategory(), pca.getRecruitmentCount()));
            }
        }
    }

    public void updateProjectSkills(List<ProjectSkill> projectSkills) {
        this.projectSkills.clear();
        for (ProjectSkill projectSkill : projectSkills) {
            this.addProjectSkill(projectSkill);
        }
    }
}
