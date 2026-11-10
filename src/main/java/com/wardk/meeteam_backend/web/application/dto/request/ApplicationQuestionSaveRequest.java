package com.wardk.meeteam_backend.web.application.dto.request;

import com.wardk.meeteam_backend.domain.application.entity.QuestionInputType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 지원서 양식(질문 목록) 저장 요청 DTO. (리더 전용)
 * 전달된 질문 목록으로 기존 양식을 전체 교체합니다.
 */
@Schema(description = "지원서 양식 저장 요청")
public record ApplicationQuestionSaveRequest(

        @Schema(description = "질문 목록 (배열 순서가 노출 순서)")
        @NotNull
        @Valid
        List<QuestionItem> questions
) {
    @Schema(description = "지원서 질문 항목")
    public record QuestionItem(

            @Schema(description = "질문 문구", example = "깃허브 주소는?")
            @NotBlank
            String label,

            @Schema(description = "입력 타입", example = "TEXT")
            @NotNull
            QuestionInputType inputType,

            @Schema(description = "필수 응답 여부", example = "true")
            boolean required,

            @Schema(description = "SELECT 타입 선택지", example = "[\"Java\", \"Kotlin\"]")
            List<String> options
    ) {
    }
}