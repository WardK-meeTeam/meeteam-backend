/*
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

    @GetMapping("/{projectId}")
    public SuccessResponse<List<ProjectMemberListResponse>> getMembers(@PathVariable Long projectId) {

        List<ProjectMemberListResponse> responses = projectMemberService.getProjectMembers(projectId);

        return SuccessResponse.of(SuccessCode._PROJECT_MEMBER_LISTED, responses);
    }

    @DeleteMapping
    public SuccessResponse<DeleteResponse> deleteMembers(@RequestBody @Validated DeleteRequest request,
                                                         @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        DeleteResponse response = projectMemberService.deleteProjectMember(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_MEMBER_DELETED, response);
    }

    @PatchMapping
    public SuccessResponse<RoleUpdateResponse> updateRole(@RequestBody @Validated RoleUpdateRequest request,
                                                          @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        RoleUpdateResponse response = projectMemberService.updateRole(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_MEMBER_ROLE_UPDATED, response);
    }

    @PatchMapping("/owner")
    public SuccessResponse<UpdateOwnerResponse> updateOwner(@RequestBody @Validated UpdateOwnerRequest request,
                                                            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        UpdateOwnerResponse response = projectMemberService.updateOwner(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_OWNER_UPDATED, response);
    }

    @DeleteMapping("/withdraw")
    public SuccessResponse<WithdrawResponse> withdraw(@RequestBody @Validated WithdrawRequest request,
                                                      @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        WithdrawResponse response = projectMemberService.withdraw(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_MEMBER_WITHDREW, response);
    }
}
*/
