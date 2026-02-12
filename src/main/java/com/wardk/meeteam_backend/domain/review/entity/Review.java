package com.wardk.meeteam_backend.domain.review.entity;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Check;

import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor
public class Review {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    //리뷰 쓴사람
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewer_id")
    private Member reviewer;

    @Column(nullable = false)
    private int projectRating;

    @Column(length = 1000)
    private String comment;

    @OneToMany(mappedBy = "review", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ReviewRecommendation> recommendations = new ArrayList<>();

    public void addRecommendation(ReviewRecommendation recommendation) {
        this.recommendations.add(recommendation);
        recommendation.assignReview(this);
    }

    @Builder
    public Review(Project project, Member reviewer, int projectRating, String comment) {
        this.project = project;
        this.reviewer = reviewer;
        this.projectRating = projectRating;
        this.comment = comment;
    }

    public static Review createReview(Project project, Member reviewer, int projectRating, String comment) {
        return Review.builder()
                .project(project)
                .reviewer(reviewer)
                .projectRating(projectRating)
                .comment(comment)
                .build();
    }
}
