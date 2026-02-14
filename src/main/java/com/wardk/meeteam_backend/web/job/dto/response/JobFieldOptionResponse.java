package com.wardk.meeteam_backend.web.job.dto.response;

import com.wardk.meeteam_backend.domain.job.entity.JobField;
import lombok.Builder;

import java.lang.reflect.Field;
import java.util.List;

@Builder
public record JobFieldOptionResponse(
        Long id,
        String code,
        String name,
        List<JobPositionOptionResponse> positions,
        List<TechStackOptionResponse> techStacks
) {
    public static JobFieldOptionResponse of(
            JobField jobField,
            List<JobPositionOptionResponse> positions,
            List<TechStackOptionResponse> techStacks
    ) {
        return new JobFieldOptionResponse(jobField.getId(), jobField.getCode(), jobField.getName(), positions, techStacks);
    }
}
