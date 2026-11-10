package com.wardk.meeteam_backend.web.application.dto.response;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationAnswer;
import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 지원자 답변 필터 검색 응답 DTO.
 */
@Schema(description = "지원자 답변 필터 검색 응답")
public record ApplicationSearchResponse(

        @Schema(description = "지원서 ID")
        Long applicationId,

        @Schema(description = "지원자 ID")
        Long applicantId,

        @Schema(description = "지원자 이름")
        String applicantName,

        @Schema(description = "지원 상태")
        String status,

        @Schema(description = "질문/답변 목록")
        List<AnswerInfo> answers
) {
    @Schema(description = "질문/답변 정보")
    public record AnswerInfo(
            @Schema(description = "질문 ID")
            Long questionId,

            @Schema(description = "질문 문구")
            String label,

            @Schema(description = "답변 값")
            String value
    ) {
    }

    public static ApplicationSearchResponse from(ProjectApplication application, List<ApplicationAnswer> answers) {
        List<AnswerInfo> answerInfos = answers.stream()
                .map(answer -> new AnswerInfo(
                        answer.getQuestion().getId(),
                        answer.getQuestion().getLabel(),
                        answer.getValue()
                ))
                .toList();

        return new ApplicationSearchResponse(
                application.getId(),
                application.getApplicant().getId(),
                application.getApplicant().getRealName(),
                application.getStatus().getDisplayName(),
                answerInfos
        );
    }
}