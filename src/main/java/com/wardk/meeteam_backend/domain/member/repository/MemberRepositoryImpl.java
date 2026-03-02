package com.wardk.meeteam_backend.domain.member.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.JPQLQuery;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.repository.Querydsl4RepositorySupport;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.support.PageableExecutionUtils;

import java.util.List;

import static com.wardk.meeteam_backend.domain.member.entity.QMember.member;
import static com.wardk.meeteam_backend.domain.member.entity.QMemberJobPosition.memberJobPosition;
import static com.wardk.meeteam_backend.domain.member.entity.QMemberTechStack.memberTechStack;
import static com.wardk.meeteam_backend.domain.projectmember.entity.QProjectMember.projectMember;
import static com.wardk.meeteam_backend.domain.job.entity.QTechStack.techStack;

@Slf4j
public class MemberRepositoryImpl extends Querydsl4RepositorySupport implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(JPAQueryFactory queryFactory) {
        super(Member.class);
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Member> searchMembers(List<Long> jobFieldIds, List<Long> skillIds, Pageable pageable) {

        // projectCount 정렬이 있는지 확인
        boolean hasProjectCountSort = pageable.getSort().stream()
                .anyMatch(order -> "projectCount".equals(order.getProperty()));

        if (hasProjectCountSort) {
            // projectCount 정렬이 있으면 수동 처리
            return searchWithProjectCountSort(jobFieldIds, skillIds, pageable);
        }

        // 일반 정렬은 applyPagination이 자동 처리
        return applyPagination(pageable, qf ->
                qf.select(member)
                        .from(member)
                        .where(
                                jobFieldExists(jobFieldIds),
                                skillExists(skillIds)
                        )
        );
    }

    /**
     * projectCount 정렬 처리 (수동 페이징 및 정렬)
     */
    private Page<Member> searchWithProjectCountSort(List<Long> jobFieldIds,
                                                    List<Long> skillIds,
                                                    Pageable pageable) {

        JPAQuery<Member> query = queryFactory
                .select(member)
                .from(member)
                .where(
                        jobFieldExists(jobFieldIds),
                        skillExists(skillIds)
                );

        // 정렬 적용
        for (Sort.Order sortOrder : pageable.getSort()) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

            if ("projectCount".equals(sortOrder.getProperty())) {
                // projectCount는 서브쿼리로 처리

                JPQLQuery<Long> projectCountExpr = JPAExpressions
                        .select(projectMember.count())
                        .from(projectMember)
                        .where(projectMember.member.eq(member));
                query.orderBy(new OrderSpecifier<>(direction, projectCountExpr));
            } else {
                // 일반 필드는 Querydsl이 처리
                getQuerydsl().applySorting(Sort.by(sortOrder), query);
            }
        }

        // 페이징 적용
        List<Member> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        // Count 쿼리
        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .where(
                        jobFieldExists(jobFieldIds),
                        skillExists(skillIds)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 대분류(JobField) 조건 (서브쿼리 EXISTS)
     * OR 조건: 여러 대분류 중 하나라도 가지고 있으면 매칭
     */
    private BooleanExpression jobFieldExists(List<Long> jobFieldIds) {
        if (jobFieldIds == null || jobFieldIds.isEmpty()) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(memberJobPosition)
                .join(memberJobPosition.jobPosition)
                .where(
                        memberJobPosition.member.eq(member),
                        memberJobPosition.jobPosition.jobField.id.in(jobFieldIds)
                )
                .exists();
    }

    /**
     * 기술스택 조건 (AND 조건)
     * 요청한 모든 스킬을 가지고 있어야 매칭
     */
    private BooleanExpression skillExists(List<Long> skillIds) {
        if (skillIds == null || skillIds.isEmpty()) {
            return null;
        }

        // 각 스킬마다 EXISTS 조건을 만들어서 AND로 연결
        BooleanExpression result = null;

        for (Long skillId : skillIds) {
            BooleanExpression skillExpr = JPAExpressions
                    .selectOne()
                    .from(memberTechStack)
                    .join(memberTechStack.techStack, techStack)
                    .where(
                            memberTechStack.member.eq(member),
                            techStack.id.eq(skillId)
                    )
                    .exists();

            result = (result == null) ? skillExpr : result.and(skillExpr);
        }

        return result;
    }
}
