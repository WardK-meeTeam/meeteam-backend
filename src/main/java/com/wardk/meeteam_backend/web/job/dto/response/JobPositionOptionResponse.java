package com.wardk.meeteam_backend.web.job.dto.response;

import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import lombok.Builder;

@Builder
public record JobPositionOptionResponse(
        Long id,
        Long fieldId,
        String code,
        String name
) {
    public static JobPositionOptionResponse of(
            JobPosition jobPosition
    ) {
        return new JobPositionOptionResponse(
                jobPosition.getId(),
                jobPosition.getJobField().getId(),
                jobPosition.getCode(),
                jobPosition.getName()
        );
    }
}
