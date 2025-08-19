package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitmentDto {

    private String subCategory;
    private int recruitmentCount;
    private int currentCount;
    private boolean isClosed;

    public static RecruitmentDto responseDto(ProjectCategoryApplication recruitment) {
        return RecruitmentDto.builder()
                .subCategory(recruitment.getSubCategory().getName())
                .recruitmentCount(recruitment.getRecruitmentCount())
                .currentCount(recruitment.getCurrentCount())
                .isClosed(recruitment.getCurrentCount() >= recruitment.getRecruitmentCount())
                .build();
    }
}
