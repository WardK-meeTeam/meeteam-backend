package com.wardk.meeteam_backend.domain.review.service;

import com.wardk.meeteam_backend.web.review.dto.request.ReviewRequest;
import com.wardk.meeteam_backend.web.review.dto.response.ReviewResponse;

public interface ReviewService {

    public ReviewResponse review(ReviewRequest request, Long reviewerId);
}
