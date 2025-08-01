package com.wardk.meeteam_backend.web.projectMember.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
public class WithdrawRequest {

    @NotNull
    private Long projectId;
}
