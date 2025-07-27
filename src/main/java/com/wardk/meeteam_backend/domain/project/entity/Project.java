package com.wardk.meeteam_backend.domain.project.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Entity
@NoArgsConstructor
public class Project extends BaseEntity {



    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_id")
    private Long id;


    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(value = EnumType.STRING)
    private ProjectStatus projectStatus;

    @Enumerated(value = EnumType.STRING)
    private PlatformCategory platformCategory;

    private String imageUrl;

    @Column(name = "offline_required", nullable = false)
    private boolean offlineRequired;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id")
    private Member creator;


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    private Category category;

    public void addCategory(Category category) {
        this.category = category;
    }


    @OneToMany(mappedBy = "project", cascade = CascadeType.REMOVE, orphanRemoval = true)
    List<ProjectMember> members = new ArrayList<>();


    @Builder
    public Project(String name, String description, PlatformCategory platformCategory, String imageUrl, boolean offlineRequired) {
        this.name = name;
        this.description = description;
        this.platformCategory = platformCategory;
        this.imageUrl = imageUrl;
        this.offlineRequired = offlineRequired;
    }

    public void joinMember(ProjectMember projectMember) {
        members.add(projectMember);
        projectMember.assignProject(this);
    }



    public void setProjectCreator(Member creator) {
        this.creator = creator;
    }

}
