package com.wardk.meeteam_backend.web.projectCategoryApplication.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class ProjectCounts {

    private Long currentCount;
    private Long recruitmentCount;


    public ProjectCounts(Long currentCount, Long recruitmentCount) {
        this.currentCount = currentCount;
        this.recruitmentCount = recruitmentCount;
    }
}
