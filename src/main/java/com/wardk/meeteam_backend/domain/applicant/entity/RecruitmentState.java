package com.wardk.meeteam_backend.domain.applicant.entity;

import com.wardk.meeteam_backend.domain.job.entity.JobField;
import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "recruitment_state")
public class RecruitmentState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_state_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_field_catalog_id", nullable = false)
    private JobField jobField;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_position_id", nullable = false)
    private JobPosition jobPosition;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "recruit_count")
    private Integer recruitmentCount;

    @Version
    @Column(name = "current_count")
    private Integer currentCount;

    @OneToMany(mappedBy = "recruitmentState", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RecruitmentTechStack> recruitmentTechStacks = new ArrayList<>();

    public void assignProject(Project project) {
        this.project = project;
    }

    public void increaseCurrentCount() {
        if (this.currentCount >= this.recruitmentCount) {
            throw new CustomException(ErrorCode.RECRUITMENT_FULL);
        }
        this.currentCount++;
    }

    public void updateCurrentCount(Integer currentCount) {
        if (currentCount < 0 || currentCount > this.recruitmentCount) {
            throw new CustomException(ErrorCode.INVALID_REQUEST);
        }
        this.currentCount = currentCount;
    }

    public void updateRecruitmentCount(Integer newRecruitmentCount) {
        if (newRecruitmentCount < 0) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_COUNT);
        }
        if (newRecruitmentCount < this.currentCount) {
            throw new CustomException(ErrorCode.INVALID_RECRUITMENT_COUNT);
        }
        this.recruitmentCount = newRecruitmentCount;
    }

    public void addRecruitmentTechStack(RecruitmentTechStack recruitmentTechStack) {
        this.recruitmentTechStacks.add(recruitmentTechStack);
        recruitmentTechStack.assignRecruitmentState(this);
    }

    @Builder
    public RecruitmentState(JobField jobField, JobPosition jobPosition, Integer recruitmentCount) {
        this.jobField = jobField;
        this.jobPosition = jobPosition;
        this.recruitmentCount = recruitmentCount;
        this.currentCount = 0;
    }

    public static RecruitmentState createRecruitmentState(JobField jobField, JobPosition jobPosition, Integer recruitmentCount) {
        return RecruitmentState.builder()
                .jobField(jobField)
                .jobPosition(jobPosition)
                .recruitmentCount(recruitmentCount)
                .build();
    }
}
