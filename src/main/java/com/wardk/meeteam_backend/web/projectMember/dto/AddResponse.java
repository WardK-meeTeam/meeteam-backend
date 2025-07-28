package com.wardk.meeteam_backend.web.projectMember.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AddResponse {

    private Long projectId;
    private Long memberId;

    public static AddResponse responseDto(Long projectId, Long memberId) {
        return AddResponse.builder()
                .projectId(projectId)
                .memberId(memberId)
                .build();
    }
}
