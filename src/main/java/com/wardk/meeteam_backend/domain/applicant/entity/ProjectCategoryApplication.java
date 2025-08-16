package com.wardk.meeteam_backend.domain.applicant.entity;


import com.wardk.meeteam_backend.domain.category.entity.SubCategory;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor
public class ProjectCategoryApplication {//project_대분류_모집

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "project_category_application_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sub_category_id")
    private SubCategory subCategory;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(name = "recruit_count")
    private Integer recruitmentCount;

    public void assignProject(Project project) {
        this.project = project;
    }

    public void decreaseRecruitmentCount() {
        if (this.recruitmentCount <= 0) {
            throw new CustomException(ErrorCode.RECRUITMENT_FULL);
        }
        this.recruitmentCount--;
    }

    @Builder
    public ProjectCategoryApplication(SubCategory subCategory, Integer recruitmentCount) {
        this.subCategory = subCategory;
        this.recruitmentCount = recruitmentCount;
    }

    public static ProjectCategoryApplication createProjectCategoryApplication(SubCategory subCategory, Integer recruitmentCount) {
        return ProjectCategoryApplication.builder()
                .subCategory(subCategory)
                .recruitmentCount(recruitmentCount).
                build();
    }
}
