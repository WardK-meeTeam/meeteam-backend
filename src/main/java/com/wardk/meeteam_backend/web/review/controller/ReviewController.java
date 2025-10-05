package com.wardk.meeteam_backend.web.review.controller;

import com.wardk.meeteam_backend.domain.review.service.ReviewService;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.response.SuccessResponse;
import com.wardk.meeteam_backend.web.review.dto.ReviewRequest;
import com.wardk.meeteam_backend.web.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    public SuccessResponse<ReviewResponse> review(@Validated @RequestBody ReviewRequest request,
                                                  @AuthenticationPrincipal CustomSecurityUserDetails userDetails) {

        ReviewResponse response = reviewService.review(request, userDetails.getMemberId());

        return SuccessResponse.onSuccess(response);
    }
}
