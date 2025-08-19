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

    @Builder
    public ProjectCategoryApplication(SubCategory subCategory, Integer recruitmentCount) {
        this.subCategory = subCategory;
        this.recruitmentCount = recruitmentCount;
        this.currentCount = 0;
    }

    public static ProjectCategoryApplication createProjectCategoryApplication(SubCategory subCategory, Integer recruitmentCount) {
        return ProjectCategoryApplication.builder()
                .subCategory(subCategory)
                .recruitmentCount(recruitmentCount).
                build();
    }
}
