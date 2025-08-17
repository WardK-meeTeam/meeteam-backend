package com.wardk.meeteam_backend.web.project.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RecruitmentDto {

    private String subCategory;
    private int recruitmentCount;
    private int currentCount;
    private boolean isClosed;

    public static RecruitmentDto responseDto(String subCategory, int recruitmentCount, int currentCount) {
        return RecruitmentDto.builder()
                .subCategory(subCategory)
                .recruitmentCount(recruitmentCount)
                .currentCount(currentCount)
                .isClosed(currentCount >= recruitmentCount)
                .build();
    }
}
