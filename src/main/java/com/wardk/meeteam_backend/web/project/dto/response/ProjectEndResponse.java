package com.wardk.meeteam_backend.web.project.dto.response;

public record ProjectEndResponse(
        Long projectId
) {
    public static ProjectEndResponse responseDto(Long projectId) {
        return new ProjectEndResponse(projectId);
    }
}
