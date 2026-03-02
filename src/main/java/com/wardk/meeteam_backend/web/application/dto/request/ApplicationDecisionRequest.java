package com.wardk.meeteam_backend.web.application.dto.request;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class ApplicationDecisionRequest {

    @NotNull(message = "결정은 필수입니다.")
    private ApplicationStatus decision;
}
