package com.wardk.meeteam_backend.domain.member.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.skill.entity.QMemberSkill;
import com.wardk.meeteam_backend.domain.skill.entity.QSkill;
import com.wardk.meeteam_backend.domain.skill.entity.Skill;
import com.wardk.meeteam_backend.global.apiPayload.code.ErrorCode;
import com.wardk.meeteam_backend.global.apiPayload.exception.CustomException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

import java.util.List;

import static com.wardk.meeteam_backend.domain.member.entity.QMember.*;
import static com.wardk.meeteam_backend.domain.skill.entity.QSkill.skill;


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
