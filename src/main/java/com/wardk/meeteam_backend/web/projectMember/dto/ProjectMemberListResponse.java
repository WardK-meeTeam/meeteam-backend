package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class ProjectMemberListResponse {

    private Long memberId;
    private String nickname;
    private String email;
    private JobType jobType;

    public static ProjectMemberListResponse responseDto(Long memberId, String nickname, String email, JobType jobType) {
        return ProjectMemberListResponse.builder()
                .memberId(memberId)
                .nickname(nickname)
                .email(email)
                .jobType(jobType)
                .build();
    }
}
