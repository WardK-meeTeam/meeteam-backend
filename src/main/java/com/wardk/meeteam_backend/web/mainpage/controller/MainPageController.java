package com.wardk.meeteam_backend.web.mainpage.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;

import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.mainpage.dto.CategoryCondition;
import com.wardk.meeteam_backend.web.mainpage.dto.ProjectConditionMainPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import java.util.Arrays;

@RequestMapping("/api/main")
@RequiredArgsConstructor
@RestController
public class MainPageController {

    private final ProjectService projectService;



    @Operation(summary = "프로젝트 메인 페이지", description = "메인 페이지에서 프로젝트 카드를 조회")
    @GetMapping("/projects")
    public SuccessResponse<Page<ProjectConditionMainPageResponse>> getProjects(
            @ParameterObject CategoryCondition condition,
            @ParameterObject Pageable pageable,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        return SuccessResponse.onSuccess(projectService.searchMainPageProject(condition, pageable, userDetails));

    }







}