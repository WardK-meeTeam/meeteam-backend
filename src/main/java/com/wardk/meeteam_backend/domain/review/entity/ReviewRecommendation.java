package com.wardk.meeteam_backend.domain.review.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@NoArgsConstructor
@Getter
public class ReviewRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_recommendation_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id")
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewee_id")
    private Member reviewee;

    public void assignReview(Review review) {
        this.review = review;
    }

    @Builder
    public ReviewRecommendation(Member reviewee) {
        this.reviewee = reviewee;
    }

    public static ReviewRecommendation createReviewRecommendation(Member reviewee){
        return ReviewRecommendation.builder()
                .reviewee(reviewee)
                .build();
    }
}
