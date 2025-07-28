package com.wardk.meeteam_backend.web.projectMember.dto;

import com.wardk.meeteam_backend.domain.member.entity.JobType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class RoleUpdateRequest {

    @NotNull
    private Long projectId;

    @NotNull
    private Long memberId;

    @NotNull
    private JobType jobType;
}
