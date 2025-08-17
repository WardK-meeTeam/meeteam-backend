package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectMemberListResponse {

    private Long memberId;
    private String name;
    private String imageUrl;
    private boolean isCreator;

    public static ProjectMemberListResponse responseDto(Long projectId, String name, String imageUrl, boolean isCreator) {
        return ProjectMemberListResponse.builder()
                .memberId(projectId)
                .name(name)
                .imageUrl(imageUrl)
                .isCreator(isCreator)
                .build();
    }
}
