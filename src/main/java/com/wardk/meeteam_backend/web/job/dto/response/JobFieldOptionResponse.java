package com.wardk.meeteam_backend.web.job.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record JobFieldOptionResponse(
        Long id,
        String code,
        String name,
        List<JobPositionOptionResponse> positions,
        List<TechStackOptionResponse> techStacks
) {
}
