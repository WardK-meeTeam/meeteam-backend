package com.wardk.meeteam_backend.web.review.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReviewResponse {

    private Long reviewId;
    private Long reviewerId;

    public static ReviewResponse responseDto(Long reviewId, Long reviewerId) {
        return ReviewResponse.builder()
                .reviewId(reviewId)
                .reviewerId(reviewerId)
                .build();
    }
}
