package com.wardk.meeteam_backend.domain.review.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wardk.meeteam_backend.domain.category.entity.QBigCategory;
import com.wardk.meeteam_backend.domain.category.entity.QSubCategory;
import com.wardk.meeteam_backend.domain.member.entity.QMember;
import com.wardk.meeteam_backend.domain.member.entity.QMemberSubCategory;
import com.wardk.meeteam_backend.domain.review.entity.QReview;
import com.wardk.meeteam_backend.web.member.dto.QReviewResponse;
import com.wardk.meeteam_backend.web.member.dto.ReviewResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class ReviewRepositoryCustomImpl implements ReviewRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<ReviewResponse> getReview(long memberId) {

        QReview review = QReview.review;
        QMember reviewer = QMember.member;
        // Removed unused Q*Category variables

        return queryFactory
                .select(new QReviewResponse(
                        reviewer.realName,
                        review.comment
                ))
                .from(review)
                // reviewerId ↔ member 조인
                .join(reviewer).on(review.revieweeId.eq(reviewer.id))
                .where(review.revieweeId.eq(memberId))
                .fetch();

    }


}
