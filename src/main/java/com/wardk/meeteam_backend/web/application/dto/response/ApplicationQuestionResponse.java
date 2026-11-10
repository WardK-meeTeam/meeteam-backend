package com.wardk.meeteam_backend.web.application.dto.response;

import com.wardk.meeteam_backend.domain.application.entity.ApplicationQuestion;
import com.wardk.meeteam_backend.domain.application.entity.QuestionInputType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * 지원서 질문 응답 DTO. (양식 렌더링용)
 */
@Schema(description = "지원서 질문 응답")
public record ApplicationQuestionResponse(

        @Schema(description = "질문 ID (답변 시 answers 의 키로 사용)")
        Long questionId,

        @Schema(description = "질문 문구")
        String label,

        @Schema(description = "입력 타입")
        QuestionInputType inputType,

        @Schema(description = "필수 응답 여부")
        boolean required,

        @Schema(description = "SELECT 타입 선택지")
        List<String> options,

        @Schema(description = "노출 순서")
        int displayOrder
) {
    public static ApplicationQuestionResponse from(ApplicationQuestion question) {
        return new ApplicationQuestionResponse(
                question.getId(),
                question.getLabel(),
                question.getInputType(),
                question.isRequired(),
                question.getOptions(),
                question.getDisplayOrder()
        );
    }
}