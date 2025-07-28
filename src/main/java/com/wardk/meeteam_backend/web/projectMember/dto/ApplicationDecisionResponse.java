package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.projectMember.entity.ApplicationStatus;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ApplicationDecisionResponse {

    private Long applicationId;
    private ApplicationStatus decision;
    private Long projectId;
    private Long memberId;


    public static ApplicationDecisionResponse rejectResponseDto(Long applicationId, ApplicationStatus decision) {
        return ApplicationDecisionResponse.builder()
                .applicationId(applicationId)
                .decision(decision)
                .build();
    }

    public static ApplicationDecisionResponse acceptResponseDto(Long applicationId, Long projectId, Long memberId, ApplicationStatus decision) {
        return ApplicationDecisionResponse.builder()
                .applicationId(applicationId)
                .projectId(projectId)
                .memberId(memberId)
                .decision(decision)
                .build();
    }
}
