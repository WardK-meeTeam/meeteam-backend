package com.wardk.meeteam_backend.domain.review.repository;

import com.wardk.meeteam_backend.domain.review.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    @Query("select r from Review r where r.revieweeId = :memberId")
    List<Review> findReview(Long memberId);
}
