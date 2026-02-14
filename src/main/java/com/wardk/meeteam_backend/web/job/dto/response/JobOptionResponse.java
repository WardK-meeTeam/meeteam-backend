package com.wardk.meeteam_backend.web.job.dto.response;

import lombok.Builder;

import java.util.List;

@Builder
public record JobOptionResponse(
        List<JobFieldOptionResponse> fields,
        List<JobPositionOptionResponse> positions,
        List<TechStackOptionResponse> techStacks
) {
}
