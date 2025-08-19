package com.wardk.meeteam_backend.web.projectMember.controller;

import com.wardk.meeteam_backend.domain.projectMember.service.ProjectApplicationService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.projectMember.dto.*;
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

    @PostMapping
    public SuccessResponse<ApplicationResponse> apply(@ModelAttribute @Validated ApplicationRequest request,
                                                      @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationResponse response = applicationService.apply(request, userDetails.getUsername());

        return SuccessResponse.onSuccess(response);
    }

    @GetMapping("/{projectId}")
    public SuccessResponse<List<ProjectApplicationListResponse>> getApplicationList(@PathVariable Long projectId,
                                                                                    @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<ProjectApplicationListResponse> applicationList = applicationService.getApplicationList(projectId, userDetails.getUsername());

        return SuccessResponse.onSuccess(applicationList);
    }

    @PostMapping("/decide")
    public SuccessResponse<ApplicationDecisionResponse> decide(@RequestBody @Validated ApplicationDecisionRequest request,
                                                               @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationDecisionResponse response = applicationService.decide(request, userDetails.getUsername());

        return SuccessResponse.onSuccess(response);
    }
}