package com.wardk.meeteam_backend.web.project.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record ProjectRecruitRequest(
        @NotNull(message = "직군 ID는 필수입니다.")
        Long jobFieldId,

        @NotNull(message = "직무 포지션 ID는 필수입니다.")
        Long jobPositionId,

        @NotNull(message = "모집 인원은 필수입니다.")
        @Min(value = 1, message = "모집 인원은 최소 1명 이상이어야 합니다.")
        Integer recruitmentCount,

        @NotEmpty(message = "모집 포지션별 기술 스택은 최소 1개 이상이어야 합니다.")
        List<Long> techStackIds
) {
}
