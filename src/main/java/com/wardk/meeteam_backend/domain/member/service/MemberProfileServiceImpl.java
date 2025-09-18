package com.wardk.meeteam_backend.domain.member.service;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepositoryCustom;
import com.wardk.meeteam_backend.domain.review.entity.Review;
import com.wardk.meeteam_backend.domain.review.repository.ReviewRepository;
import com.wardk.meeteam_backend.domain.review.repository.ReviewRepositoryCustom;
import com.wardk.meeteam_backend.global.exception.CustomException;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.web.member.dto.MemberProfileResponse;
import com.wardk.meeteam_backend.web.member.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MemberProfileServiceImpl implements MemberProfileService {

    private final MemberRepository memberRepository;
    private final MemberRepositoryCustom memberRepositoryCustom;
    private final ReviewRepository reviewRepository;
    private final ReviewRepositoryCustom reviewRepositoryCustom;

    /**
     * 나의 프로필
     * @param memberId
     * @return
     */
    @Override
    @Transactional(readOnly = true)
    public MemberProfileResponse profile(Long memberId) {

        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEMBER_NOT_FOUND));

        MemberProfileResponse memberProfileResponse = new MemberProfileResponse(member);

        List<ReviewResponse> reviewResponses = reviewRepositoryCustom.getReview(member.getId());
        memberProfileResponse.setReviewList(reviewResponses);
        memberProfileResponse.setReviewCount(reviewResponses.size());


        return memberProfileResponse;

    }
}
