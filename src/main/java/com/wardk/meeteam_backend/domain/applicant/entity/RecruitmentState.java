package com.wardk.meeteam_backend.domain.applicant.entity;


import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "recruitment_state")
public class RecruitmentState {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recruitment_state_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "recruit_count")
    private Integer recruitmentCount;

    @Version
    @Column(name = "current_count")
    private Integer currentCount;

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

    @Builder
    public RecruitmentState(SubCategory subCategory, Integer recruitmentCount) {
        this.subCategory = subCategory;
        this.recruitmentCount = recruitmentCount;
        this.currentCount = 0;
    }

    public static RecruitmentState createRecruitmentState(SubCategory subCategory, Integer recruitmentCount) {
        return RecruitmentState.builder()
                .subCategory(subCategory)
                .recruitmentCount(recruitmentCount)
                .build();
    }
}