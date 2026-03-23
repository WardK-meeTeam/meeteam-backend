package com.wardk.meeteam_backend.domain.project.service;

import com.wardk.meeteam_backend.web.project.dto.request.ProjectRepoRequest;
import com.wardk.meeteam_backend.web.project.dto.response.ProjectRepoResponse;

import java.util.List;

/**
 * 프로젝트 GitHub 레포지토리 연동 관련 비즈니스 로직을 정의하는 서비스 인터페이스.
 */
public interface ProjectRepoService {

    /**
     * 프로젝트에 GitHub 레포지토리를 연결합니다.
     * 리더만 연결 가능합니다.
     *
     * @param projectId 프로젝트 ID
     * @param request 레포지토리 연결 요청
     * @param requesterEmail 요청자 이메일
     * @return 연결된 레포지토리 정보 목록
     */
    List<ProjectRepoResponse> addRepo(Long projectId, ProjectRepoRequest request, String requesterEmail);

    /**
     * 프로젝트에 연결된 레포지토리 목록을 조회합니다.
     *
     * @param projectId 프로젝트 ID
     * @return 레포지토리 정보 목록
     */
    List<ProjectRepoResponse> findRepos(Long projectId);
}