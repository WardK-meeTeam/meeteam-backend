package com.wardk.meeteam_backend.domain.application.repository;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ApplicationQuestionRepository extends JpaRepository<ApplicationQuestion, Long> {

    /**
     * 프로젝트의 지원서 질문을 노출 순서대로 조회합니다. (양식 렌더링용)
     */
    List<ApplicationQuestion> findByProjectIdOrderByDisplayOrderAsc(Long projectId);

    /**
     * 프로젝트의 기존 지원서 질문을 모두 삭제합니다. (양식 교체 시)
     */
    void deleteByProjectId(Long projectId);
}