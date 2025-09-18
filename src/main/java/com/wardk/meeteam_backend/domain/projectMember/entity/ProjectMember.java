package com.wardk.meeteam_backend.domain.projectMember.entity;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.category.entity.BigCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ProjectMember extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_member_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member member;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    public void assignProject(Project project) {
        this.project = project;
    }

    @Builder
    public ProjectMember(Member member, SubCategory subCategory) {
        this.member = member;
        this.subCategory = subCategory;
    }

    public static ProjectMember createProjectMember(Member member, SubCategory subCategory) {
        return ProjectMember.builder()
                .member(member)
                .subCategory(subCategory)
                .build();
    }
}
