package com.wardk.meeteam_backend.domain.application.repository;

import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.web.application.dto.request.ApplicationSearchRequest;

import java.util.List;

/**
 * 지원자 답변(EAV) 기반 동적 필터 검색 레포지토리.
 */
public interface ApplicationSearchRepository {

    /**
     * 필터 조건을 모두(AND) 만족하는 대기중 지원서를 조회합니다.
     */
    List<ProjectApplication> search(Long projectId, List<ApplicationSearchRequest.FilterCondition> filters);
}
