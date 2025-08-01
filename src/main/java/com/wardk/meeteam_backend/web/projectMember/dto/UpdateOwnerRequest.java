package com.wardk.meeteam_backend.web.projectMember.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class UpdateOwnerRequest {

    @NotNull
    private Long projectId;

    @NotNull
    private Long newOwnerId; // memberId
}
