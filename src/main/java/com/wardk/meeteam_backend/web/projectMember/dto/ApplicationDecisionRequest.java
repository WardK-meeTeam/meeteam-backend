package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.projectMember.entity.ApplicationStatus;
import lombok.Getter;

@Getter
public class ApplicationDecisionRequest {

    private Long applicationId;
    private ApplicationStatus decision;
}
