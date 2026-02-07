package com.wardk.meeteam_backend.web.project.dto.request;

import com.wardk.meeteam_backend.domain.job.JobPosition;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record ProjectRecruitRequest(
        @NotNull(message = "직무 포지션은 필수입니다.")
        JobPosition jobPosition,

        @NotNull(message = "모집 인원은 필수입니다.")
        @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.")
        Integer recruitmentCount
) {
}