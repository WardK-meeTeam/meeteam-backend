package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.domain.project.service.dto.ProjectPostCommand;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.mainpage.dto.request.CategoryCondition;
import com.wardk.meeteam_backend.web.mainpage.dto.response.ProjectConditionMainPageResponse;
import com.wardk.meeteam_backend.web.project.dto.request.*;
import com.wardk.meeteam_backend.web.project.dto.response.*;
import com.wardk.meeteam_backend.web.projectmember.dto.response.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 프로젝트 관련 비즈니스 로직을 정의하는 서비스 인터페이스.
 */
public interface ProjectService {

    /**
     * 새 프로젝트를 생성합니다.
     */
    ProjectPostResponse createProject(ProjectPostCommand command, MultipartFile file, String requesterEmail);

    /**
     * 모든 프로젝트 목록을 조회합니다.
     */
    List<ProjectListResponse> findAllProjects();

    /**
     * 프로젝트를 수정합니다.
     */
    ProjectUpdateResponse updateProject(Long projectId, ProjectUpdateRequest request, MultipartFile file, String requesterEmail);

    /**
     * 프로젝트를 삭제합니다.
     */
    ProjectDeleteResponse deleteProject(Long projectId, String requesterEmail);

    /**
     * 프로젝트에 GitHub 레포지토리를 연결합니다.
     */
    List<ProjectRepoResponse> addRepo(Long projectId, ProjectRepoRequest request, String requesterEmail);

    /**
     * 사용자가 참여 중인/완료한 프로젝트 목록을 조회합니다.
     */
    List<MyProjectResponse> findMyProjects(CustomSecurityUserDetails userDetails);

    /**
     * 조건에 맞는 프로젝트를 검색합니다.
     */
    Page<ProjectConditionRequest> searchProjects(ProjectSearchCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails);

    /**
     * 메인 페이지용 프로젝트 목록을 조회합니다.
     */
    Page<ProjectConditionMainPageResponse> searchMainPageProjects(CategoryCondition condition, Pageable pageable, CustomSecurityUserDetails userDetails);

    /**
     * 프로젝트 상세 정보를 조회합니다.
     *
     * @param projectId 프로젝트 ID
     * @param memberId  현재 로그인한 사용자 ID (비로그인 시 null)
     */
    ProjectDetailResponse findProjectById(Long projectId, Long memberId);

    /**
     * 프로젝트에 연결된 레포지토리 목록을 조회합니다.
     */
    List<ProjectRepoResponse> findProjectRepos(Long projectId);

    /**
     * 프로젝트를 종료합니다.
     */
    ProjectEndResponse endProject(Long projectId, String requesterEmail);
}

