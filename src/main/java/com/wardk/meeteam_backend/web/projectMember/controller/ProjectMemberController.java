package com.wardk.meeteam_backend.web.projectMember.controller;

import com.wardk.meeteam_backend.domain.projectMember.service.ProjectMemberService;
import com.wardk.meeteam_backend.global.apiPayload.code.SuccessCode;
import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/project-members")
public class ProjectMemberController {

    private final ProjectMemberService projectMemberService;

    @DeleteMapping
    public SuccessResponse<DeleteResponse> deleteMembers(@RequestBody @Validated DeleteRequest request,
                                                         @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        DeleteResponse response = projectMemberService.deleteProjectMember(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_MEMBER_DELETED, response);
    }

    @DeleteMapping("/withdraw")
    public SuccessResponse<WithdrawResponse> withdraw(@RequestBody @Validated WithdrawRequest request,
                                                      @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        WithdrawResponse response = projectMemberService.withdraw(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_MEMBER_WITHDREW, response);
    }
}
