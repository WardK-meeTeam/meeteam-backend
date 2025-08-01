package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class RoleUpdateResponse {

    private Long projectId;
    private Long memberId;
    private JobType jobType;

    public static RoleUpdateResponse responseDto(Long projectId, Long memberId, JobType jobType) {
        return RoleUpdateResponse.builder()
                .projectId(projectId)
                .memberId(memberId)
                .jobType(jobType)
                .build();
    }
}
