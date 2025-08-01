package com.wardk.meeteam_backend.domain.projectMember.entity;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
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

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne
    @JoinColumn(name = "member_id")
    private Member member;

    @Enumerated(value = EnumType.STRING)
    private JobType jobType;


    public void assignMember(Member member) {
        this.member = member;
    }

    public void assignProject(Project project) {
        this.project = project;
    }

    public void updateJobType(JobType jobType) {
        this.jobType = jobType;
    }

    @Builder
    public ProjectMember(JobType jobType) {
        this.jobType = jobType;
    }
}
