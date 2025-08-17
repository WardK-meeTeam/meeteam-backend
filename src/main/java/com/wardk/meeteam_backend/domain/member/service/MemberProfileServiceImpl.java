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
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberRepositoryCustom memberRepositoryCustom;
    private final ReviewRepository reviewRepository;
    private final ReviewRepositoryCustom reviewRepositoryCustom;

    @Override
    @Transactional(readOnly = true)
    public MemberProfileResponse profile(Long memberId) {

        Member member = memberRepositoryCustom.getProfile(memberId);

        MemberProfileResponse memberProfileResponse = new MemberProfileResponse(member);

        List<ReviewResponse> reviewResponses = reviewRepositoryCustom.getReview(member.getId());
        memberProfileResponse.setReviewList(reviewResponses);
        memberProfileResponse.setReviewCount(reviewResponses.size());


        return memberProfileResponse;

    }
}
