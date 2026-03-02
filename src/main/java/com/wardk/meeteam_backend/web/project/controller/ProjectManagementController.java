package com.wardk.meeteam_backend.web.project.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectRepoRequest;
import com.wardk.meeteam_backend.web.project.dto.request.ProjectUpdateRequest;
import com.wardk.meeteam_backend.web.project.dto.response.ProjectRepoResponse;
import com.wardk.meeteam_backend.web.project.dto.response.RecruitmentStatusResponse;
import com.wardk.meeteam_backend.web.project.dto.response.TeamManagementResponse;
import com.wardk.meeteam_backend.web.projectmember.dto.response.ProjectUpdateResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 프로젝트 관리 페이지 관련 API 컨트롤러.
 * 프로젝트 수정, 모집 상태 관리, 레포지토리 연결 등을 담당합니다.
 */
@Tag(name = "Project Management", description = "프로젝트 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectManagementController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 수정", description = "프로젝트 정보를 수정합니다.")
    @PostMapping(value = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ProjectUpdateResponse> updateProject(
            @PathVariable Long projectId,
            @Parameter(description = "프로젝트 수정 정보", required = true)
            @RequestPart @Validated ProjectUpdateRequest request,
            @Parameter(description = "프로젝트 대표 이미지", required = false)
            @RequestPart(name = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        ProjectUpdateResponse response = projectService.updateProject(
                projectId, request, file, userDetails.getUsername()
        );
        return SuccessResponse.onSuccess(response);
    }

    @Operation(summary = "모집 상태 토글", description = "프로젝트 모집 상태를 토글합니다. (모집중 ↔ 모집중단)")
    @PostMapping("/{projectId}/recruitment/toggle")
    public SuccessResponse<RecruitmentStatusResponse> toggleRecruitmentStatus(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        RecruitmentStatusResponse response = projectService.toggleRecruitmentStatus(projectId, userDetails.getUsername());
        return SuccessResponse.onSuccess(response);
    }

    @Operation(summary = "GitHub 레포지토리 연결", description = "프로젝트에 GitHub 레포지토리를 연결합니다.")
    @PostMapping("/{projectId}/repos")
    public SuccessResponse<List<ProjectRepoResponse>> addRepository(
            @PathVariable Long projectId,
            @RequestBody @Validated ProjectRepoRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        List<ProjectRepoResponse> responses = projectService.addRepo(projectId, request, userDetails.getUsername());
        return SuccessResponse.onSuccess(responses);
    }

    @Operation(summary = "프로젝트 레포지토리 조회", description = "프로젝트에 연결된 레포지토리 목록을 조회합니다.")
    @GetMapping("/{projectId}/repos")
    public SuccessResponse<List<ProjectRepoResponse>> findProjectRepos(@PathVariable Long projectId) {
        List<ProjectRepoResponse> responses = projectService.findProjectRepos(projectId);
        return SuccessResponse.onSuccess(responses);
    }

    @Operation(summary = "팀원 관리 정보 조회", description = "프로젝트 팀원 관리 페이지에 필요한 정보를 조회합니다. 현재 멤버 수, 총 모집 정원, 대기중인 지원서 수, 팀원 목록을 포함합니다.")
    @GetMapping("/{projectId}/team")
    public SuccessResponse<TeamManagementResponse> getTeamManagement(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        TeamManagementResponse response = projectService.getTeamManagement(projectId, userDetails.getUsername());
        return SuccessResponse.onSuccess(response);
    }
}