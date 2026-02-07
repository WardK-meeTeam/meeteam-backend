
package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.mainpage.dto.request.CategoryCondition;
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectConditionMainPageResponse;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
import com.wardk.meeteam_backend.web.projectlike.dto.response.ProjectWithLikeDto;
import com.wardk.meeteam_backend.web.projectmember.dto.request.*;
import com.wardk.meeteam_backend.web.projectmember.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface ProjectService {

    ProjectPostResponse postProject(ProjectPostRequest projectPostRequest, MultipartFile file, String requesterEmail);
    List<ProjectListResponse> getProjectList();
    ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail);
    ProjectDeleteResponse deleteProject(Long projectId, String requesterEmail);
    List<ProjectRepoResponse> addRepo(Long projectId, ProjectRepoRequest request, String requesterEmail);


    // 참여중, 종료된 프로젝트 조회
    List<MyProjectResponse> getMyProject(CustomSecurityUserDetails userDetails);
    Page<ProjectConditionRequest> searchProject(ProjectSearchCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails);


    //메인 페이지 조회
    Page<ProjectConditionMainPageResponse> searchMainPageProject(CategoryCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails);

//    ProjectResponse getProjectV1(Long projectId);
    ProjectWithLikeDto getProjectV2(Long projectId);

    // 프로젝트 레포지토리 조회
    List<ProjectRepoResponse> getProjectRepos(Long projectId);

    // 프로젝트 종료
    ProjectEndResponse endProject(Long projectId, String requesterEmail);


}

