package com.wardk.meeteam_backend.web.project.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.global.apiPayload.response.SuccessResponse;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostRequsetDto;
import com.wardk.meeteam_backend.web.project.dto.ProjectPostResponseDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@Slf4j
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    @PostMapping(value = "/api/project/post", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ProjectPostResponseDto> projectPost(
            @ModelAttribute @Validated ProjectPostRequsetDto projectPostRequsetDto,
            @RequestPart(name = "file", required = false) MultipartFile file
    ) {

        ProjectPostResponseDto projectPostResponseDto = projectService.postProject(projectPostRequsetDto, file);

        return SuccessResponse.onSuccess(projectPostResponseDto);

    }
}
