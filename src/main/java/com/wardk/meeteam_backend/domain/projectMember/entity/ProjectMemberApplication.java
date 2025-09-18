package com.wardk.meeteam_backend.domain.projectMember.entity;

import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.notification.entity.NotificationType;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectMemberApplication extends BaseEntity { // 프로젝트 지원서

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
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "project_application_weekday",
            joinColumns = @JoinColumn(name = "project_application_id")
    )
    @Enumerated(EnumType.STRING)
    @Column(name = "week_day")
    @Builder.Default
    private List<WeekDay> weekDays = new ArrayList<>();

    //오프라인/온라인 여부
    private boolean offlineAvailable;

    //프로젝트 지원서 상태
    @Enumerated(value = EnumType.STRING)
    @Builder.Default
    private ApplicationStatus status = ApplicationStatus.PENDING;

    @Builder
    public ProjectMemberApplication(Project project, Member applicant, SubCategory subCategory, String motivation,
                                    int availableHoursPerWeek, List<WeekDay> weekDays, boolean offlineAvailable) {
        this.project = project;
        this.applicant = applicant;
        this.subCategory = subCategory;
        this.motivation = motivation;
        this.availableHoursPerWeek = availableHoursPerWeek;
        this.weekDays = weekDays;
        this.offlineAvailable = offlineAvailable;
    }

    public static ProjectMemberApplication createApplication(Project project, Member applicant, SubCategory subCategory,
                                                             String motivation, int availableHoursPerWeek, List<WeekDay> weekDays,
                                                             boolean offlineAvailable) {
        return ProjectMemberApplication.builder()
                .project(project)
                .applicant(applicant)
                .subCategory(subCategory)
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
