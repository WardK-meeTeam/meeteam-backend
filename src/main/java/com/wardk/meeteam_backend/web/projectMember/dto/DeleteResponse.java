package com.wardk.meeteam_backend.web.projectMember.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeleteResponse {

    private Long projectId;
    private Long memberId;
    private String removedMemberEmail;

    public static DeleteResponse responseDto(Long projectId, Long memberId, String removedMemberEmail) {
        return DeleteResponse.builder()
                .projectId(projectId)
                .memberId(memberId)
                .removedMemberEmail(removedMemberEmail)
                .build();
    }
}
