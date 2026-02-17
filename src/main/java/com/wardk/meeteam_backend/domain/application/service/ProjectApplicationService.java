package com.wardk.meeteam_backend.domain.application.service;

import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.web.application.dto.request.*;
import com.wardk.meeteam_backend.web.application.dto.response.*;

import java.util.List;

/**
 * 프로젝트 지원 서비스 인터페이스.
 */
public interface ProjectApplicationService {

    /**
     * 지원서 폼 정보 조회.
     * 지원자 정보와 프로젝트의 모집 포지션 목록을 반환합니다.
     */
    ApplicationFormResponse getApplicationForm(Long projectId, Long memberId);

    /**
     * 프로젝트 지원.
     */
    ApplicationResponse apply(Long projectId, Long memberId, ApplicationRequest request);

    /**
     * 프로젝트 지원자 목록 조회 (리더 전용).
     */
    List<ProjectApplicationListResponse> getApplicationList(Long projectId, String requesterEmail);

    /**
     * 프로젝트 지원 상세 조회 (리더 전용).
     */
    ApplicationDetailResponse getApplicationDetail(Long projectId, Long applicationId, String requesterEmail);

    /**
     * 지원자 승인/거절 (리더 전용).
     */
    ApplicationDecisionResponse decide(ApplicationDecisionRequest request, String requesterEmail);

    /**
     * 내가 지원한 프로젝트 목록 조회.
     */
    List<AppliedProjectResponse> getAppliedProjects(CustomSecurityUserDetails userDetails);
}
