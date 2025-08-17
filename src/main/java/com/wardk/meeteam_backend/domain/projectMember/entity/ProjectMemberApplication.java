package com.wardk.meeteam_backend.domain.projectMember.entity;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    //지원 동기, 자기소개
    @Column(length = 800)
    private String motivation;

    //주당 투자 가능 시간
    private int availableHoursPerWeek;

    //참가 가능한 요일
    @Enumerated(EnumType.STRING)
    private WeekDay weekDay;

    //오프라인/온라인 여부
    private boolean offlineAvailable;

    //프로젝트 지원서 상태
    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Builder
    public ProjectMemberApplication(Project project, Member applicant, SubCategory subCategory, String motivation,
                                    int availableHoursPerWeek, WeekDay weekDay, boolean offlineAvailable) {
        this.project = project;
        this.applicant = applicant;
        this.subCategory = subCategory;
        this.motivation = motivation;
        this.availableHoursPerWeek = availableHoursPerWeek;
        this.weekDay = weekDay;
        this.offlineAvailable = offlineAvailable;
    }

    public static ProjectMemberApplication createApplication(Project project, Member applicant, SubCategory subCategory,
                                                              String motivation, int availableHoursPerWeek, WeekDay weekDay,
                                                              boolean offlineAvailable) {
        return ProjectMemberApplication.builder()
                .project(project)
                .applicant(applicant)
                .subCategory(subCategory)
                .motivation(motivation)
                .availableHoursPerWeek(availableHoursPerWeek)
                .weekDay(weekDay)
                .offlineAvailable(offlineAvailable)
                .build();
    }
    public void updateStatus(ApplicationStatus status) {
        this.status = status;
    }
}
