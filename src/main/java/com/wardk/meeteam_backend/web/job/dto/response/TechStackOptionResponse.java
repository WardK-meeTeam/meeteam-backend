package com.wardk.meeteam_backend.web.job.dto.response;

import lombok.Builder;

@Builder
public record TechStackOptionResponse(
        Long id,
        String name
) {
}
