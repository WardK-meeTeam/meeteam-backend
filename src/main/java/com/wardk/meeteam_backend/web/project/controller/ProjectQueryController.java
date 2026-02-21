package com.wardk.meeteam_backend.web.project.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ProjectQueryController {


    private final ProjectService projectService;

    @Operation(summary = "프로젝트 조건 검색", description = "조건과 페이징을 기반으로 프로젝트 목록을 조회합니다.")
    @GetMapping("api/projects/condition")
    public Page<ProjectConditionRequest> searchCondition(
            @AuthenticationPrincipal(errorOnInvalidType = false) CustomSecurityUserDetails userDetails,
            @ParameterObject ProjectSearchCondition condition,
            @ParameterObject Pageable pageable) {

        return projectService.searchProjects(condition, pageable, userDetails);
    }


}
