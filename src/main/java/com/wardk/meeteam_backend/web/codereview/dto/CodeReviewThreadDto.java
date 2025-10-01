package com.wardk.meeteam_backend.web.codereview.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 코드리뷰 스레드 정보 DTO
 */
@Data
@Builder
@AllArgsConstructor
public class CodeReviewThreadDto {
    private Long id;
    private String title;
    private String filename;
    private Integer lineNumber;
    private String codeSnippet;
    private Long creatorId;
    private Boolean isResolved;
    private LocalDateTime resolvedAt;
    private Long resolvedBy;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
