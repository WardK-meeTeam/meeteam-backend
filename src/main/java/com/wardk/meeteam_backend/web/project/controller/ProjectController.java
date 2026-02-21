package com.wardk.meeteam_backend.web.project.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
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
 * 프로젝트 관련 API 컨트롤러.
 */
@Tag(name = "Project", description = "프로젝트 관리 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/projects")
public class ProjectController {

    private final ProjectService projectService;

    @Operation(summary = "프로젝트 생성",
            description = "새 프로젝트를 생성합니다. 직군/직무/기술스택 정보는 GET /api/jobs/options를 먼저 호출하여 조회하고, 선택한 직군(JobField)에 해당하는 기술스택만 전송해야 합니다. 마감 방식: END_DATE(마감 날짜 기반, endDate 필수), RECRUITMENT_COMPLETED(모집 완료 시, endDate 미전송)")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ProjectPostResponse> createProject(
            @Parameter(description = "프로젝트 생성 정보", required = true)
            @RequestPart @Validated ProjectPostRequest request,
            @Parameter(description = "프로젝트 대표 이미지", required = false)
            @RequestPart(name = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        ProjectPostResponse response = projectService.createProject(request.toCommand(), file, userDetails.getUsername());
        return SuccessResponse.onSuccess(response);
    }




    @Operation(summary = "프로젝트 목록 조회", description = "전체 프로젝트 목록을 조회합니다.")
    @GetMapping
    public SuccessResponse<List<ProjectListResponse>> findAllProjects() {
        List<ProjectListResponse> projects = projectService.findAllProjects();
        return SuccessResponse.onSuccess(projects);
    }

    @Operation(summary = "프로젝트 상세 조회", description = "프로젝트 상세 정보를 조회합니다. 프로젝트 리더의 관심분야와 기술스택 정보, 좋아요 수, 현재 사용자의 좋아요/리더 여부를 포함합니다.")
    @GetMapping("/{projectId}")
    public SuccessResponse<ProjectDetailResponse> findProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        Long memberId = userDetails != null ? userDetails.getMemberId() : null;
        ProjectDetailResponse project = projectService.findProjectById(projectId, memberId);
        return SuccessResponse.onSuccess(project);
    }



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

    @Operation(summary = "프로젝트 삭제", description = "프로젝트를 삭제합니다.")
    @DeleteMapping("/{projectId}")
    public SuccessResponse<ProjectDeleteResponse> deleteProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        ProjectDeleteResponse response = projectService.deleteProject(projectId, userDetails.getUsername());
        return SuccessResponse.onSuccess(response);
    }

    @Operation(summary = "모집 상태 토글", description = "프로젝트 모집 상태를 토글합니다. (모집중 ↔ 모집완료)")
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



    @Operation(summary = "내 프로젝트 조회", description = "로그인한 사용자가 참여 중인/완료한 프로젝트를 조회합니다.")
    @GetMapping("/my")
    public SuccessResponse<List<MyProjectResponse>> findMyProjects(
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        List<MyProjectResponse> myProjects = projectService.findMyProjects(userDetails);
        return SuccessResponse.onSuccess(myProjects);
    }
}
