package com.wardk.meeteam_backend.web.projectCategoryApplication.dto;

import lombok.Data;
import lombok.Getter;

@Data
@Getter
public class ProjectCounts {

    private Long totalCount;
    private Long totalRecruitmentCount;


    public ProjectCounts(Long totalCount, Long totalRecruitmentCount) {
        this.totalCount = totalCount;
        this.totalRecruitmentCount = totalRecruitmentCount;
    }
}
