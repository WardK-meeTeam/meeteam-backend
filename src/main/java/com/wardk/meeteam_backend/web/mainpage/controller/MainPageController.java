package com.wardk.meeteam_backend.web.mainpage.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;

import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.mainpage.dto.request.CategoryCondition;
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectConditionMainPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/main")
@RequiredArgsConstructor
@RestController
public class MainPageController {

    private final ProjectService projectService;



    @Operation(summary = "프로젝트 메인 페이지", description = "메인 페이지에서 프로젝트 카드를 조회")
    @GetMapping("/projects")
    public SuccessResponse<Page<ProjectConditionMainPageResponse>> getProjects(
            @ParameterObject CategoryCondition condition,
            @ParameterObject @PageableDefault(
                    page = 0,
                    size = 20,
                    sort = "createdAt",
                    direction = Sort.Direction.DESC
            ) Pageable pageable,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        return SuccessResponse.onSuccess(projectService.searchMainPageProjects(condition, pageable, userDetails));

    }

    // ...existing code...

}