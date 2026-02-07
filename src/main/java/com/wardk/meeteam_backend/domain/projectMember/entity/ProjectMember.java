package com.wardk.meeteam_backend.domain.projectmember.entity;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "project_member")
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

    @Enumerated(EnumType.STRING)
    @Column(name = "job_position", nullable = false)
    private JobPosition jobPosition;

    public void assignProject(Project project) {
        this.project = project;
    }

    @Builder
    public ProjectMember(Member member, JobPosition jobPosition) {
        this.member = member;
        this.jobPosition = jobPosition;
    }

    public static ProjectMember createProjectMember(Member member, JobPosition jobPosition) {
        return ProjectMember.builder()
                .member(member)
                .jobPosition(jobPosition)
                .build();
    }
}