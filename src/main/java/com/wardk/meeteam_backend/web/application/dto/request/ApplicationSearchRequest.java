package com.wardk.meeteam_backend.web.application.dto.request;

import com.wardk.meeteam_backend.domain.application.entity.FilterOperator;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

/**
 * 지원자 답변 필터 검색 요청 DTO. (리더 전용)
 *
 * 리더가 자신이 정의한 질문을 기준으로 지원자를 필터링합니다.
 * 모든 조건을 AND 로 만족하는 지원자만 조회됩니다.
 */
@Schema(description = "지원자 답변 필터 검색 요청")
public record ApplicationSearchRequest(

        @Schema(description = "필터 조건 목록 (AND 결합). 비어 있으면 전체 대기 지원자 조회")
        @Valid
        List<FilterCondition> filters
) {
    @Schema(description = "필터 조건")
    public record FilterCondition(

            @Schema(description = "필터 기준 질문 ID", example = "2")
            @NotNull
            Long questionId,

            @Schema(description = "연산자 (NUMBER: GTE/LTE, SELECT: EQ, TEXT: CONTAINS)", example = "GTE")
            @NotNull
            FilterOperator operator,

            @Schema(description = "비교 값", example = "20")
            @NotBlank
            String value
    ) {
    }
}