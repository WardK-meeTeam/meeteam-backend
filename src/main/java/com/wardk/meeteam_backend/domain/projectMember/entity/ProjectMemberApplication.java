package com.wardk.meeteam_backend.domain.projectMember.entity;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberApplication { // 프로젝트 지원서

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

    //주당 투자 가능 시간
    private int availableHoursPerWeek;

    //참가 가능한 요일
    private WeekDay weekDay;

    //오프라인/온라인 여부
    private boolean offlineAvailable;


    //프로젝트 지원서 상태
    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    public void updateStatus(ApplicationStatus status) {
        this.status = status;
    }
}
