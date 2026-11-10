package com.wardk.meeteam_backend.domain.application.repository;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ApplicationAnswerRepository extends JpaRepository<ApplicationAnswer, Long> {

    /**
     * 특정 지원서의 답변을 질문과 함께 노출 순서대로 조회합니다. (리더 상세 조회용)
     */
    @Query("""
            SELECT a FROM ApplicationAnswer a
            JOIN FETCH a.question q
            WHERE a.application.id = :applicationId
            ORDER BY q.displayOrder ASC
            """)
    List<ApplicationAnswer> findByApplicationIdWithQuestion(Long applicationId);
}