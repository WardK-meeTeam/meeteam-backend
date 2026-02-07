package com.wardk.meeteam_backend.web.auth.dto.register;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "직무 포지션 요청")
public record JobPositionRequest(
        @Schema(description = "직무 포지션", example = "WEB_SERVER")
        JobPosition jobPosition
) {
}
