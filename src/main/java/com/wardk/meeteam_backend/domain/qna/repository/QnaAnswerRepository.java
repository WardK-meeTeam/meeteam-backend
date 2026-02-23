package com.wardk.meeteam_backend.domain.qna.repository;

import com.wardk.meeteam_backend.domain.qna.entity.QnaAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Q&A 답변 레포지토리.
 */
public interface QnaAnswerRepository extends JpaRepository<QnaAnswer, Long> {
}
