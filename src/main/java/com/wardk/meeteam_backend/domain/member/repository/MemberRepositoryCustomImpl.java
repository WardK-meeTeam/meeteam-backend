package com.wardk.meeteam_backend.domain.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.response.ErrorCode;
import com.wardk.meeteam_backend.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import static com.wardk.meeteam_backend.domain.member.entity.QMember.member;


@Component
@RequiredArgsConstructor
public class MemberRepositoryCustomImpl implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;



    @Override
    public Member getProfile(Long memberId) {

        Member findMember = queryFactory
                .select(member)
                .from(member)
                .where(member.id.eq(memberId))
                .fetchOne();

        if (findMember == null) {
            throw new CustomException(ErrorCode.MEMBER_NOT_FOUND);
        }
        return findMember;
    }




}
