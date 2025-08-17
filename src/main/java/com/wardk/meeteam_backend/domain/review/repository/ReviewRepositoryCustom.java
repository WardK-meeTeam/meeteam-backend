package com.wardk.meeteam_backend.domain.review.repository;

import com.wardk.meeteam_backend.web.member.dto.ReviewResponse;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ReviewRepositoryCustom {

    List<ReviewResponse> getReview(long memberId);
}
