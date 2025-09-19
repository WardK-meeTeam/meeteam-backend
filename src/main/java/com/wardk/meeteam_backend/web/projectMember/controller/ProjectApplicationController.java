package com.wardk.meeteam_backend.web.projectMember.controller;

import com.wardk.meeteam_backend.domain.projectMember.service.ProjectApplicationService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import org.springframework.http.MediaType;
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
    @io.swagger.v3.oas.annotations.parameters.RequestBody(required = true,
            content = @Content(mediaType = MediaType.APPLICATION_FORM_URLENCODED_VALUE,
                    schema = @Schema(implementation = ApplicationRequest.class)))
    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
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



    @Operation(summary = "프로젝트 지원 상세 조회")
    @GetMapping("/{projectId}/{applicationId}")
    public SuccessResponse<ApplicationDetailResponse> getApplicationDetail(@PathVariable Long projectId, @PathVariable Long applicationId,
                                                                           @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationDetailResponse applicationDetail = applicationService.getApplicationDetail(projectId, applicationId, userDetails.getUsername());

        return SuccessResponse.onSuccess(applicationDetail);
    }




    @Operation(summary = "내가 지원한 프로젝트 조회")
    @GetMapping("/my")
    public SuccessResponse<List<AppliedProjectResponse>> getMyAppliedProjects(@AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<AppliedProjectResponse> appliedProjects = applicationService.getAppliedProjects(userDetails);

        return SuccessResponse.onSuccess(appliedProjects);
    }



}