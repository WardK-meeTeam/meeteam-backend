package com.wardk.meeteam_backend.web.review.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class ReviewRequest {

    @NotNull
    private Long projectId;

    @NotNull
    private Integer projectRating;

    @NotBlank
    private String comment;

    private List<Long> recommendedMemberIds;
}
