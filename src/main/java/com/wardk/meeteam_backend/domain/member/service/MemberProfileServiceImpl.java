package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepositoryCustom;
import com.wardk.meeteam_backend.domain.review.entity.Review;
import com.wardk.meeteam_backend.domain.review.repository.ReviewRepository;
import com.wardk.meeteam_backend.domain.review.repository.ReviewRepositoryCustom;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;
import com.wardk.meeteam_backend.web.member.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberRepositoryCustom memberRepositoryCustom;
    private final ReviewRepository reviewRepository;
    private final ReviewRepositoryCustom reviewRepositoryCustom;

    @Override
    public MemberProfileResponse profile(Long memberId) {

        Member member = memberRepositoryCustom.getProfile(memberId);

        //리뷰 카운트
        List<Review> review = reviewRepository.findReview(memberId);
        MemberProfileResponse memberProfileResponse = new MemberProfileResponse(member);
        memberProfileResponse.setReviewCount(review.size());

        List<ReviewResponse> review1 = reviewRepositoryCustom.getReview(member.getId());
        memberProfileResponse.setReviewList(review1);


        return memberProfileResponse;

    }
}
