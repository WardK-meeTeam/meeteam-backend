package com.wardk.meeteam_backend.domain.application.service;

import com.wardk.meeteam_backend.web.application.dto.request.ApplicationQuestionSaveRequest;
import com.wardk.meeteam_backend.web.application.dto.response.ApplicationQuestionResponse;

import java.util.List;

/**
 * 지원서 양식(질문) 서비스 인터페이스.
 */
public interface ApplicationQuestionService {

    /**
     * 프로젝트의 지원서 양식을 저장합니다. (리더 전용, 전체 교체)
     */
    List<ApplicationQuestionResponse> saveQuestions(Long projectId, String requesterEmail,
                                                    ApplicationQuestionSaveRequest request);

    /**
     * 프로젝트의 지원서 질문 목록을 조회합니다. (양식 렌더링용)
     */
    List<ApplicationQuestionResponse> getQuestions(Long projectId);
}