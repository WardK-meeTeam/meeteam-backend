package com.wardk.meeteam_backend.web.projectMember.controller;

import com.wardk.meeteam_backend.domain.projectMember.service.ProjectApplicationService;
import com.wardk.meeteam_backend.global.apiPayload.code.SuccessCode;
import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationRequest;
import com.wardk.meeteam_backend.web.projectMember.dto.ApplicationResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/prject-application")
public class ProjectApplicationController {

    private final ProjectApplicationService applicationService;

    @PostMapping
    public SuccessResponse<ApplicationResponse> apply(@ModelAttribute @Validated ApplicationRequest request,
                                                      @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ApplicationResponse response = applicationService.apply(request, userDetails.getUsername());

        return SuccessResponse.of(SuccessCode._PROJECT_APPLICATION_CREATED, response);
    }
}
