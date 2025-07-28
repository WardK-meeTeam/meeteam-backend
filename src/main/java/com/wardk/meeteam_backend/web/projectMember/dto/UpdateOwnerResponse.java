package com.wardk.meeteam_backend.web.projectMember.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class UpdateOwnerResponse {

    private Long projectId;
    private Long oldOwnerId; // memberId
    private Long newOwnerId; // memberId

    public static UpdateOwnerResponse responseDto(Long projectId, Long oldOwnerId, Long newOwnerId) {
        return UpdateOwnerResponse.builder()
                .projectId(projectId)
                .oldOwnerId(oldOwnerId)
                .newOwnerId(newOwnerId)
                .build();
    }
}
