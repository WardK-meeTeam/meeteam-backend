package com.wardk.meeteam_backend.domain.projectMember.entity;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectApplication {

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

    @Enumerated(value = EnumType.STRING)
    private JobType jobType;

    @Column(length = 800)
    private String motivation;

    private int availableHoursPerWeek;

    private String availableDays;

    private boolean offlineAvailable;

    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    public void updateStatus(ApplicationStatus status) {
        this.status = status;
    }
}
