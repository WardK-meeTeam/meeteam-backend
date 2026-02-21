package com.wardk.meeteam_backend.web.application.controller;

import com.wardk.meeteam_backend.domain.application.service.ProjectApplicationService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.application.dto.request.*;
import com.wardk.meeteam_backend.web.application.dto.response.*;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "프로젝트 지원", description = "프로젝트 지원 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectApplicationController {

    private final ProjectApplicationService applicationService;

    @Operation(summary = "프로젝트 지원", description = "프로젝트에 지원합니다. 프로젝트 리더는 자신의 프로젝트에 지원할 수 없습니다.")
    @PostMapping("/{projectId}/application")
    public SuccessResponse<ApplicationResponse> apply(
            @PathVariable Long projectId,
            @RequestBody @Validated ApplicationRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationResponse response = applicationService.apply(projectId, userDetails.getMemberId(), request);

        return SuccessResponse.onSuccess(response);
    }

    @Operation(summary = "프로젝트 지원자 목록 조회", description = "프로젝트 리더가 지원자 목록을 조회합니다.")
    @GetMapping("/{projectId}/applications")
    public SuccessResponse<List<ProjectApplicationListResponse>> getApplicationList(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<ProjectApplicationListResponse> applicationList = applicationService.getApplicationList(projectId, userDetails.getUsername());
        return SuccessResponse.onSuccess(applicationList);
    }

    @Operation(summary = "프로젝트 지원 상세 조회", description = "프로젝트 리더가 특정 지원서의 상세 정보를 조회합니다.")
    @GetMapping("/{projectId}/applications/{applicationId}")
    public SuccessResponse<ApplicationDetailResponse> getApplicationDetail(
            @PathVariable Long projectId,
            @PathVariable Long applicationId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationDetailResponse applicationDetail = applicationService.getApplicationDetail(projectId, applicationId, userDetails.getUsername());

        return SuccessResponse.onSuccess(applicationDetail);
    }

    @Operation(summary = "프로젝트 지원자 승인/거절", description = "프로젝트 리더가 지원자를 승인하거나 거절합니다.")
    @PostMapping("/{projectId}/applications/{applicationId}/decision")
    public SuccessResponse<ApplicationDecisionResponse> decide(
            @PathVariable Long projectId,
            @PathVariable Long applicationId,
            @RequestBody @Validated ApplicationDecisionRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationDecisionResponse response = applicationService.decide(request, userDetails.getUsername());

        return SuccessResponse.onSuccess(response);
    }

    @Operation(summary = "내가 지원한 프로젝트 조회", description = "내가 지원한 프로젝트 목록을 조회합니다.")
    @GetMapping("/my/applications")
    public SuccessResponse<List<AppliedProjectResponse>> getMyAppliedProjects(
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<AppliedProjectResponse> appliedProjects = applicationService.getAppliedProjects(userDetails);

        return SuccessResponse.onSuccess(appliedProjects);
    }
}
