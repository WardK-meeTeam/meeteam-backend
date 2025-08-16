package com.wardk.meeteam_backend.web.project.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostRequestDto;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/project")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ProjectPostResponseDto> projectPost(
            @RequestPart @Validated ProjectPostRequestDto projectPostRequestDto,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
            ) {
        log.info("Project Post Request: {}", projectPostRequestDto);
        ProjectPostResponseDto projectPostResponseDto = projectService.postProject(projectPostRequestDto, file, userDetails.getUsername());

        return SuccessResponse.onSuccess(projectPostResponseDto);

    }
}