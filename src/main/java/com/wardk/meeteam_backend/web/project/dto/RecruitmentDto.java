package com.wardk.meeteam_backend.web.project.dto;

import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitmentDto {

    private String bigCategory;
    private String subCategory;
    private int recruitmentCount;
    private int currentCount;
    private boolean isClosed;

    public static RecruitmentDto responseDto(RecruitmentState recruitment) {
        return RecruitmentDto.builder()
                .bigCategory(recruitment.getSubCategory().getBigCategory().getName())
                .subCategory(recruitment.getSubCategory().getName())
                .recruitmentCount(recruitment.getRecruitmentCount())
                .currentCount(recruitment.getCurrentCount())
                .isClosed(recruitment.getCurrentCount() >= recruitment.getRecruitmentCount())
                .build();
    }
}
