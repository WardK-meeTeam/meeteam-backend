package com.wardk.meeteam_backend.domain.review.repository;

import com.wardk.meeteam_backend.domain.review.entity.ReviewRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRecommendationRepository extends JpaRepository<ReviewRecommendation,Long> {
}
