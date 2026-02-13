package com.wardk.meeteam_backend.web.project.controller;

import com.wardk.meeteam_backend.domain.project.service.ProjectService;
import com.wardk.meeteam_backend.domain.project.service.dto.ProjectPostCommand;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
import com.wardk.meeteam_backend.web.projectlike.dto.response.ProjectWithLikeDto;
import com.wardk.meeteam_backend.web.projectmember.dto.response.*;
import io.swagger.v3.oas.annotations.Operation;
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

    @Operation(summary = "프로젝트 등록")
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ProjectPostResponse> projectPost(
            @RequestPart @Validated ProjectPostRequest projectPostRequest,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        ProjectPostResponse projectPostResponse = projectService.postProject(
                ProjectPostCommand.from(projectPostRequest),
                file,
                userDetails.getUsername());
        return SuccessResponse.onSuccess(projectPostResponse);
    }

    @Operation(summary = "프로젝트 목록 조회")
    @GetMapping
    public SuccessResponse<List<ProjectListResponse>> getProjectList() {

        List<ProjectListResponse> projectList = projectService.getProjectList();

        return SuccessResponse.onSuccess(projectList);
    }

//    @Operation(summary = "프로젝트 상세 조회")
//    @GetMapping("V1/{projectId}")
//    public SuccessResponse<ProjectResponse> getProjectV1(@PathVariable Long projectId) {
//
//        ProjectResponse projectResponse = projectService.getProjectV1(projectId);
//        return SuccessResponse.onSuccess(projectResponse);
//    }

    @Operation(summary = "프로젝트 상세 조회")
    @GetMapping("V2/{projectId}")
    public SuccessResponse<ProjectWithLikeDto> getProjectV2(@PathVariable Long projectId) {

        ProjectWithLikeDto projectV1 = projectService.getProjectV2(projectId);
        return SuccessResponse.onSuccess(projectV1);
    }

    @Operation(summary = "프로젝트 수정")
    @PostMapping(value = "/{projectId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public SuccessResponse<ProjectUpdateResponse> updateProject(
            @RequestPart @Validated ProjectUpdateRequest projectUpdateRequest,
            @RequestPart(name = "file", required = false) MultipartFile file,
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {

        ProjectUpdateResponse projectUpdateResponse = projectService.updateProject(projectId, projectUpdateRequest, file, userDetails.getUsername());

        return SuccessResponse.onSuccess(projectUpdateResponse);
    }

    @Operation(summary = "프로젝트 삭제")
    @DeleteMapping("/{projectId}")
    public SuccessResponse<ProjectDeleteResponse> deleteProject(@PathVariable Long projectId,
                                                                @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ProjectDeleteResponse projectDeleteResponse = projectService.deleteProject(projectId, userDetails.getUsername());

        return SuccessResponse.onSuccess(projectDeleteResponse);
    }

    @Operation(summary = "프로젝트에 레포 추가")
    @PostMapping("/{projectId}/repos")
    public SuccessResponse<List<ProjectRepoResponse>> addRepo(
            @PathVariable Long projectId,
            @RequestBody @Validated ProjectRepoRequest request,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {

        List<ProjectRepoResponse> responses = projectService.addRepo(projectId, request, userDetails.getUsername());

        return SuccessResponse.onSuccess(responses);
    }

    @Operation(summary = "내가 진행 중인/완료한 프로젝트 조회")
    @GetMapping("my")
    public SuccessResponse<List<MyProjectResponse>> getMyProjects(@AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        List<MyProjectResponse> myProjects = projectService.getMyProject(userDetails);

        return SuccessResponse.onSuccess(myProjects);
    }

    // 프로젝트의 연결된 레포 목록 조회
    @Operation(summary = "프로젝트의 연결된 레포 목록 조회")
    @GetMapping("/{projectId}/repos")
    public SuccessResponse<List<ProjectRepoResponse>> getProjectRepos(@PathVariable Long projectId) {
        List<ProjectRepoResponse> responses = projectService.getProjectRepos(projectId);
        return SuccessResponse.onSuccess(responses);
    }

    @Operation(summary = "프로젝트 종료")
    @PostMapping("/{projectId}/end")
    public SuccessResponse<ProjectEndResponse> endProject(
            @PathVariable Long projectId,
            @AuthenticationPrincipal CustomSecurityUserDetails userDetails
    ) {
        ProjectEndResponse projectEndResponse = projectService.endProject(projectId, userDetails.getUsername());
        return SuccessResponse.onSuccess(projectEndResponse);
    }
}