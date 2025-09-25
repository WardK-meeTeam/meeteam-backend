
package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.mainpage.dto.MainPageProjectDto;
import com.wardk.meeteam_backend.web.mainpage.dto.SliceResponse;
import com.wardk.meeteam_backend.web.project.dto.*;
import com.wardk.meeteam_backend.web.projectLike.dto.ProjectWithLikeDto;
import com.wardk.meeteam_backend.web.projectMember.dto.ProjectUpdateResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {

    ProjectPostResponse postProject(ProjectPostRequest projectPostRequest, MultipartFile file, String requesterEmail);
    List<ProjectListResponse> getProjectList();
    ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail);
    ProjectDeleteResponse deleteProject(Long projectId, String requesterEmail);
    List<ProjectRepoResponse> addRepo(Long projectId, ProjectRepoRequest request, String requesterEmail);

    // 메인 페이지용 메서드
    @Transactional(readOnly = true)
    SliceResponse<MainPageProjectDto> getRecruitingProjectsByCategory(Long bigCategoryId, Pageable pageable);


    // 참여중, 종료된 프로젝트 조회
    List<MyProjectResponse> getMyProject(CustomSecurityUserDetails userDetails);
    Page<ProjectConditionRequest> searchProject(ProjectSearchCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails);


    ProjectResponse getProjectV1(Long projectId);
    ProjectWithLikeDto getProjectV2(Long projectId);
}

