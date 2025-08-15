/*
package com.wardk.meeteam_backend.web.projectMember.controller;

import com.wardk.meeteam_backend.domain.projectMember.service.ProjectApplicationService;
import com.wardk.meeteam_backend.global.apiPayload.code.SuccessCode;
import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationDecisionRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationDecisionResponse;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/project-application")
public class ProjectApplicationController {

    private final ProjectApplicationService applicationService;

    @PostMapping
    public SuccessResponse<ApplicationResponse> apply(@ModelAttribute @Validated ApplicationRequest request,
                                                      @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationResponse response = applicationService.apply(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_APPLICATION_CREATED, response);
    }

    @PostMapping("/decide")
    public SuccessResponse<ApplicationDecisionResponse> decide(@RequestBody @Validated ApplicationDecisionRequest request,
                                                               @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationDecisionResponse response = applicationService.decide(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_APPLICATION_DECIDED, response);
    }
}
*/
