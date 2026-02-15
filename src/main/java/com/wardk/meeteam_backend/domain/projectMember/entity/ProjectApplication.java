package com.wardk.meeteam_backend.domain.projectmember.entity;


import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(name = "project_member_application")
public class ProjectApplication extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "member_id")
    private Member applicant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_position_id")
    private JobPosition jobPosition;

    @Column(length = 800)
    private String motivation;

    private int availableHoursPerWeek;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "project_application_weekday",
            joinColumns = @JoinColumn(name = "project_application_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "week_day")
    @Builder.Default
    private List<WeekDay> weekDays = new ArrayList<>();

    private boolean offlineAvailable;

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Builder
    public ProjectApplication(Project project, Member applicant, JobPosition jobPosition, String motivation,
                                    int availableHoursPerWeek, List<WeekDay> weekDays, boolean offlineAvailable) {
        this.project = project;
        this.applicant = applicant;
        this.jobPosition = jobPosition;
        this.motivation = motivation;
        this.availableHoursPerWeek = availableHoursPerWeek;
        this.weekDays = weekDays;
        this.offlineAvailable = offlineAvailable;
    }

    public static ProjectApplication createApplication(Project project, Member applicant, JobPosition jobPosition,
                                                             String motivation, int availableHoursPerWeek, List<WeekDay> weekDays,
                                                             boolean offlineAvailable) {
        return ProjectApplication.builder()
                .project(project)
                .applicant(applicant)
                .jobPosition(jobPosition)
                .motivation(motivation)
                .availableHoursPerWeek(availableHoursPerWeek)
                .weekDays(weekDays)
                .offlineAvailable(offlineAvailable)
                .build();
    }

    public void updateStatus(ApplicationStatus status) {
        this.status = status;
    }
}