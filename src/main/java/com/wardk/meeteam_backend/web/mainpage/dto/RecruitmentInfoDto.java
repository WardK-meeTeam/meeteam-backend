package com.wardk.meeteam_backend.web.mainpage.dto;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecruitmentInfoDto { // 모집 현황 정보
    private String subCategoryName;
    private Integer recruitmentCount;
    private Integer currentCount;

    /**
     * ProjectCategoryApplication을 RecruitmentInfoDto로 변환하는 정적 메서드
     */
    public static RecruitmentInfoDto responseDto(ProjectCategoryApplication recruitment) {
        return RecruitmentInfoDto.builder()
                .subCategoryName(recruitment.getSubCategory().getName())
                .recruitmentCount(recruitment.getRecruitmentCount())
                .currentCount(recruitment.getCurrentCount())
                .build();
    }
}