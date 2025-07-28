package com.wardk.meeteam_backend.web.projectMember.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationResponse {

    private Long id;
    private Long projectId;
    private Long applicantId;

    public static ApplicationResponse responseDto(Long id, Long projectId, Long applicantId) {
        return ApplicationResponse.builder()
                .id(id)
                .projectId(projectId)
                .applicantId(applicantId)
                .build();
    }
}
