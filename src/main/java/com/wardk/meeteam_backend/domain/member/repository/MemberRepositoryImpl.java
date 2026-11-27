package com.wardk.meeteam_backend.domain.member.repository;

import com.querydsl.core.types.Order;
import com.querydsl.core.types.OrderSpecifier;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.JPAExpressions;
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

@Slf4j
public class MemberRepositoryImpl extends Querydsl4RepositorySupport implements MemberRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    public MemberRepositoryImpl(JPAQueryFactory queryFactory) {
        super(Member.class);
        this.queryFactory = queryFactory;
    }

    @Override
    public Page<Member> searchMembersV1(String name, Long jobFieldId, List<Long> techStackIds, Pageable pageable) {
        // projectCount 정렬이 있는지 확인
        boolean hasProjectCountSort = pageable.getSort().stream()
                .anyMatch(order -> "projectCount".equals(order.getProperty()));

        if (hasProjectCountSort) {
            return searchMembersV1WithProjectCountSort(name, jobFieldId, techStackIds, pageable);
        }

        return applyPagination(pageable, qf ->
                qf.select(member)
                        .from(member)
                        .where(
                                member.deletedAt.isNull(),
                                nameContains(name),
                                jobFieldIdEquals(jobFieldId),
                                techStackIdsExist(techStackIds)
                        )
        );
    }

    /**
     * v1 검색 + projectCount 정렬 처리
     */
    private Page<Member> searchMembersV1WithProjectCountSort(String name, Long jobFieldId,
                                                              List<Long> techStackIds, Pageable pageable) {
        JPAQuery<Member> query = queryFactory
                .select(member)
                .from(member)
                .where(
                        member.deletedAt.isNull(),
                        nameContains(name),
                        jobFieldIdEquals(jobFieldId),
                        techStackIdsExist(techStackIds)
                );

        // 정렬 적용
        for (Sort.Order sortOrder : pageable.getSort()) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

            if ("projectCount".equals(sortOrder.getProperty())) {
                // 비정규화 컬럼 정렬 — 상관 스칼라 서브쿼리 제거(P5)
                query.orderBy(new OrderSpecifier<>(direction, member.projectCount));
            } else {
                getQuerydsl().applySorting(Sort.by(sortOrder), query);
            }
        }

        List<Member> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .where(
                        member.deletedAt.isNull(),
                        nameContains(name),
                        jobFieldIdEquals(jobFieldId),
                        techStackIdsExist(techStackIds)
                );

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    /**
     * 이름 검색 (부분 일치).
     */
    private BooleanExpression nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return member.realName.containsIgnoreCase(name);
    }

    /**
     * 직군 ID 조건.
     */
    private BooleanExpression jobFieldIdEquals(Long jobFieldId) {
        if (jobFieldId == null) {
            return null;
        }

        return JPAExpressions
                .selectOne()
                .from(memberJobPosition)
                .join(memberJobPosition.jobPosition)
                .where(
                        memberJobPosition.member.eq(member),
                        memberJobPosition.jobPosition.jobField.id.eq(jobFieldId)
                )
                .exists();
    }

    /**
     * 기술스택 ID 조건 (AND 조건) — 스택별 상관 EXISTS.
     * 요청한 모든 기술스택을 보유한 회원만 매칭한다.
     * 스택 하나라도 보유하지 않으면 즉시 탈락(short-circuit)하며,
     * 커버링 인덱스 (tech_stack_id, member_id)로 각 EXISTS가 index-only seek으로 처리된다.
     * EXISTS는 행의 존재 여부만 보므로 유니크 제약 적용 전의 레거시 중복 행에도 안전하다.
     * 기술스택 이름은 서비스 레이어에서 ID로 선변환되어 들어온다.
     */
    private BooleanExpression techStackIdsExist(List<Long> techStackIds) {
        if (techStackIds == null || techStackIds.isEmpty()) {
            return null;
        }

        BooleanExpression predicate = null;
        for (Long techStackId : techStackIds) {
            BooleanExpression exists = JPAExpressions
                    .selectOne()
                    .from(memberTechStack)
                    .where(
                            memberTechStack.member.eq(member),
                            memberTechStack.techStack.id.eq(techStackId)
                    )
                    .exists();
            predicate = (predicate == null) ? exists : predicate.and(exists);
        }
        return predicate;
    }

    @Override
    public Page<Member> findAllWithProjectCountSort(Pageable pageable) {
        // projectCount 정렬이 있는지 확인
        boolean hasProjectCountSort = pageable.getSort().stream()
                .anyMatch(order -> "projectCount".equals(order.getProperty()));

        if (!hasProjectCountSort) {
            // projectCount 정렬이 없으면 기본 applyPagination 사용
            return applyPagination(pageable, qf ->
                    qf.select(member)
                            .from(member)
                            .where(member.deletedAt.isNull())
            );
        }

        // projectCount 정렬 처리
        JPAQuery<Member> query = queryFactory
                .select(member)
                .from(member)
                .where(member.deletedAt.isNull());

        for (Sort.Order sortOrder : pageable.getSort()) {
            Order direction = sortOrder.isAscending() ? Order.ASC : Order.DESC;

            if ("projectCount".equals(sortOrder.getProperty())) {
                // 비정규화 컬럼 정렬 — 상관 스칼라 서브쿼리 제거(P5)
                query.orderBy(new OrderSpecifier<>(direction, member.projectCount));
            } else {
                getQuerydsl().applySorting(Sort.by(sortOrder), query);
            }
        }

        List<Member> content = query
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .fetch();

        JPAQuery<Long> countQuery = queryFactory
                .select(member.count())
                .from(member)
                .where(member.deletedAt.isNull());

        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }
}
