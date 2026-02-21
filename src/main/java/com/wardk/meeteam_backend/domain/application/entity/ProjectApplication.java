package com.wardk.meeteam_backend.domain.application.entity;


import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 프로젝트 지원서 엔티티.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "project_member_application")
public class ProjectApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id", nullable = false)
    private Member applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id", nullable = false)
    private JobPosition jobPosition;

    @Column(length = 800)
    private String motivation;

    @Enumerated(value = EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status;

    @Builder
    private ProjectApplication(Project project, Member applicant, JobPosition jobPosition, String motivation) {
        this.project = project;
        this.applicant = applicant;
        this.jobPosition = jobPosition;
        this.motivation = motivation;
        this.status = ApplicationStatus.PENDING;
    }

    public static ProjectApplication create(Project project, Member applicant, JobPosition jobPosition, String motivation) {
        return ProjectApplication.builder()
                .project(project)
                .applicant(applicant)
                .jobPosition(jobPosition)
                .motivation(motivation)
                .build();
    }

    public void updateStatus(ApplicationStatus status) {
        this.status = status;
    }
}
