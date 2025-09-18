package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.projectMember.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ApplicationDecisionRequest {

    @NotNull(message = "지원서 ID는 필수입니다.")
    private Long applicationId;

    @NotNull(message = "결정은 필수입니다.")
    private ApplicationStatus decision;
}
