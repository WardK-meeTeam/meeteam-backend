package com.wardk.meeteam_backend.web.projectMember.controller;

import com.wardk.meeteam_backend.domain.projectMember.service.ProjectApplicationService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects-application")
public class ProjectApplicationController {

    private final ProjectApplicationService applicationService;

    @Operation(summary = "프로젝트 지원")
    @PostMapping
    public SuccessResponse<ApplicationResponse> apply(@ModelAttribute @Validated ApplicationRequest request,
                                                      @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationResponse response = applicationService.apply(request, userDetails.getUsername());

        return SuccessResponse.onSuccess(response);
    }

    @Operation(summary = "프로젝트 지원자 목록 조회")
    @GetMapping("/{projectId}")
    public SuccessResponse<List<ProjectApplicationListResponse>> getApplicationList(@PathVariable Long projectId,
                                                                                    @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<ProjectApplicationListResponse> applicationList = applicationService.getApplicationList(projectId, userDetails.getUsername());

        return SuccessResponse.onSuccess(applicationList);
    }

    @Operation(summary = "프로젝트 지원자 승인/거절")
    @PostMapping("/decide")
    public SuccessResponse<ApplicationDecisionResponse> decide(@RequestBody @Validated ApplicationDecisionRequest request,
                                                               @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationDecisionResponse response = applicationService.decide(request, userDetails.getUsername());

        return SuccessResponse.onSuccess(response);
    }

    @PostMapping("/{projectId}/{applicationId}")
    public SuccessResponse<ApplicationDetailResponse> getApplicationDetail(@PathVariable Long projectId, @PathVariable Long applicationId,
                                                                           @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationDetailResponse applicationDetail = applicationService.getApplicationDetail(projectId, applicationId, userDetails.getUsername());

        return SuccessResponse.onSuccess(applicationDetail);
    }

    @GetMapping("/my")
    public SuccessResponse<List<AppliedProjectResponse>> getMyAppliedProjects(@AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<AppliedProjectResponse> appliedProjects = applicationService.getAppliedProjects(userDetails.getUsername());

        return SuccessResponse.onSuccess(appliedProjects);
    }
}