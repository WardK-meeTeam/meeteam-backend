package com.wardk.meeteam_backend.web.project.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.project.dto.ProjectListResponse;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostRequest;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostResponse;
import com.wardk.meeteam_backend.web.project.dto.ProjectResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ProjectPostResponse> projectPost(
            @RequestPart @Validated ProjectPostRequest projectPostRequest,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
            ) {
        log.info("Project Post Request: {}", projectPostRequest);
        ProjectPostResponse projectPostResponse = projectService.postProject(projectPostRequest, file, userDetails.getUsername());

        return SuccessResponse.onSuccess(projectPostResponse);

    }

    @GetMapping
    public SuccessResponse<List<ProjectListResponse>> getProjectList() {

        List<ProjectListResponse> projectList = projectService.getProjectList();

        return SuccessResponse.onSuccess(projectList);
    }

    @GetMapping("/{projectId}")
    public SuccessResponse<ProjectResponse> getProject(@PathVariable Long projectId) {

        ProjectResponse projectResponse = projectService.getProject(projectId);
        return SuccessResponse.onSuccess(projectResponse);
    }
}