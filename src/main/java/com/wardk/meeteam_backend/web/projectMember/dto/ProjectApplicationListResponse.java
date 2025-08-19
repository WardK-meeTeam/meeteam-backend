package com.wardk.meeteam_backend.web.projectMember.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectApplicationListResponse {

    private Long applicationId;
    private Long applicantId;
    private String applicantName;
    private String subCategoryName;

    public static ProjectApplicationListResponse responseDto(Long applicationId, Long applicantId,
                                                             String applicantName, String subCategoryName) {
        return ProjectApplicationListResponse.builder()
                .applicationId(applicationId)
                .applicantId(applicantId)
                .applicantName(applicantName)
                .subCategoryName(subCategoryName)
                .build();
    }
}
