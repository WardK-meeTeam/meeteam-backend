package com.wardk.meeteam_backend.web.qna.dto.response;

import com.wardk.meeteam_backend.domain.qna.entity.ProjectQna;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Q&A 응답 DTO.
 */
@Schema(description = "Q&A 정보")
public record ProjectQnaResponse(
        @Schema(description = "Q&A ID")
        Long qnaId,

        // 질문 정보
        @Schema(description = "질문자 ID")
        Long questionerId,

        @Schema(description = "질문자 이름")
        String questionerName,

        @Schema(description = "질문자 프로필 이미지 URL")
        String questionerProfileImageUrl,

        @Schema(description = "질문 내용")
        String question,

        @Schema(description = "질문 등록 시간")
        LocalDateTime createdAt,

        // 답변 목록
        @Schema(description = "답변 목록")
        List<QnaAnswerResponse> answers
) {
    public static ProjectQnaResponse from(ProjectQna qna, Long leaderId) {
        List<QnaAnswerResponse> answerResponses = qna.getAnswers().stream()
                .map(answer -> QnaAnswerResponse.from(answer, leaderId))
                .toList();

        return new ProjectQnaResponse(
                qna.getId(),
                qna.getQuestioner().getId(),
                qna.getQuestioner().getRealName(),
                qna.getQuestioner().getStoreFileName(),
                qna.getQuestion(),
                qna.getCreatedAt(),
                answerResponses
        );
    }
}
