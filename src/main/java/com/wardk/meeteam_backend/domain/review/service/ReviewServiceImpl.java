package com.wardk.meeteam_backend.domain.review.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectStatus;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.projectMember.repository.ProjectMemberRepository;
import com.wardk.meeteam_backend.domain.review.entity.Review;
import com.wardk.meeteam_backend.domain.review.entity.ReviewRecommendation;
import com.wardk.meeteam_backend.domain.review.repository.ReviewRepository;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.review.dto.ReviewRequest;
import com.wardk.meeteam_backend.web.review.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReviewServiceImpl implements ReviewService {

    private final ProjectRepository projectRepository;
    private final MemberRepository memberRepository;
    private final ProjectMemberRepository projectMemberRepository;
    private final ReviewRepository reviewRepository;

    @Override
    public ReviewResponse review(ReviewRequest request, Long reviewerId) {

        Project project = projectRepository.findById(request.getProjectId())
                .orElseThrow(() -> new CustomException(ErrorCode.PROJECT_NOT_FOUND));

        if (project.getStatus() != ProjectStatus.COMPLETED) {
            throw new CustomException(ErrorCode.PROJECT_NOT_COMPLETED);
        }

        if (!projectMemberRepository.existsByProjectIdAndMemberId(request.getProjectId(), reviewerId)) {
            throw new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
        }

        Member reviewer = memberRepository.findById(reviewerId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        if (reviewRepository.existsByProjectAndReviewer(project, reviewer)) {
            throw new CustomException(ErrorCode.ALREADY_REVIEWED);
        }


        Review review = Review.createReview(project, reviewer, request.getProjectRating(), request.getComment());

        if (request.getRecommendedMemberIds() != null) {
            for (Long recommendedId : request.getRecommendedMemberIds()) {
                if (!projectMemberRepository.existsByProjectIdAndMemberId(request.getProjectId(), recommendedId)) {
                    throw new CustomException(ErrorCode.PROJECT_MEMBER_NOT_FOUND);
                }

                Member recommended = memberRepository.findById(recommendedId)
                        .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

                ReviewRecommendation reviewRecommendation = ReviewRecommendation.createReviewRecommendation(recommended);

                review.addRecommendation(reviewRecommendation);
            }
        }

        Review saved = reviewRepository.save(review);

        return ReviewResponse.responseDto(saved.getId(), reviewerId);
    }
}
