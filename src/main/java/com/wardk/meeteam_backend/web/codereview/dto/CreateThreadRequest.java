package com.wardk.meeteam_backend.web.codereview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * 코드리뷰 스레드 생성 요청 DTO
 */
@Data
@AllArgsConstructor
public class CreateThreadRequest {
    @NotNull(message = "라인 번호는 필수입니다")
    private Integer lineNumber;

    @NotBlank(message = "스레드 제목은 필수입니다")
    private String title;

    private String codeSnippet;
    private String initialMessage;
}
