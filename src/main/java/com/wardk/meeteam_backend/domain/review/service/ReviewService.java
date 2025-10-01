package com.wardk.meeteam_backend.domain.review.service;

import com.wardk.meeteam_backend.web.review.dto.ReviewRequest;
import com.wardk.meeteam_backend.web.review.dto.ReviewResponse;

public interface ReviewService {

    public ReviewResponse review(ReviewRequest request, Long reviewerId);
}
