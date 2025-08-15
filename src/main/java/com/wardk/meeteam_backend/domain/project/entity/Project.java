package com.wardk.meeteam_backend.domain.project.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.domain.review.Review;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.security.core.parameters.P;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class Project extends BaseEntity {



    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;


    @Column(name = "project_name")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "image_url")
    private String imageUrl;

    @Enumerated(value = EnumType.STRING)
    private ProjectStatus status;

    @Enumerated(value = EnumType.STRING)
    private PlatformCategory platformCategory;

    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProjectSkill> projectSkills = new ArrayList<>();


    @Column(name = "offline_required", nullable = false)
    private boolean offlineRequired;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Member creator;

    @Enumerated(EnumType.STRING)
    private ProjectCategory category;

    private LocalDate startDate;

    private LocalDate endDate;

    private Boolean isDeleted;


    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    List<ProjectMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "project")
    List<Review> reviews = new ArrayList<>();


    @Builder
    public Project(String name, String description, PlatformCategory platformCategory, String imageUrl, boolean offlineRequired, Member creator) {
        this.name = name;
        this.description = description;
        this.platformCategory = platformCategory;
        this.imageUrl = imageUrl;
        this.offlineRequired = offlineRequired;
        this.creator = creator;
    }

    public static Project createProject(String name, String description, PlatformCategory platformCategory, String imageUrl, boolean offlineRequired, Member creator) {
        return Project.builder()
                .name(name)
                .description(description)
                .platformCategory(platformCategory)
                .imageUrl(imageUrl)
                .offlineRequired(offlineRequired)
                .creator(creator)
                .build();
    }

    public void joinMember(ProjectMember projectMember) {
        members.add(projectMember);
        projectMember.assignProject(this);
    }

}
