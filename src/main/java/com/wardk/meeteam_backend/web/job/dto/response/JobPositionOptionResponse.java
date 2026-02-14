package com.wardk.meeteam_backend.web.job.dto.response;

import lombok.Builder;

@Builder
public record JobPositionOptionResponse(
        Long id,
        Long fieldId,
        String code,
        String name
) {
}
