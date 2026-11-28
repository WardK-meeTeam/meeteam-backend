package com.wardk.meeteam_backend.domain.member.search;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 기술스택 OR 매칭 팀원 검색 — 검색 전용 투영 테이블({@code member_stack_search}) 조회.
 * <p>
 * 이전 구현(member ↔ member_tech_stack 비상관 IN 세미조인)은 옵티마이저에게
 * 드라이빙 방향·세미조인 전략의 선택권을 남겼고, 실측에서 그 선택이 규모에 따라
 * 틀어졌다(230k 회원에서 Materialize 선택 51.9ms vs FirstMatch 0.116ms — 450배,
 * docs/멤버검색.md 실험 기록). 투영 테이블 조회는 스택별로 인덱스
 * {@code idx_mss_stack_pcount}를 정렬 순서 그대로 (offset+limit)행만 읽으므로
 * 옵티마이저가 선택할 것이 없어, 어떤 스택 분포에서도 비용 상한이 보장된다.
 * <p>
 * 정렬은 projectCount 내림차순으로 고정이다 — 투영 테이블이 그 순서로만 존재하기
 * 때문이며, 다른 정렬 축이 필요해지면 투영 테이블(인덱스) 추가가 선행돼야 한다.
 */
@Repository
@RequiredArgsConstructor
public class MemberTechStackSearchRepository {

    private final EntityManager em;

    /**
     * 요청한 기술스택 중 하나라도 보유한 회원을 projectCount 내림차순으로 페이징 조회한다.
     * <p>
     * 쿼리 구조 (한 문장 — DB 왕복 1회):
     * <ol>
     *   <li>스택별로 투영 테이블에서 상위 (offset+limit)명을 인덱스 순서로 읽는다.
     *       전체 상위 N명은 반드시 자기가 보유한 어느 스택의 상위 N명 안에 있으므로
     *       이 UNION은 정답의 상위집합이다.</li>
     *   <li>UNION이 (member_id, project_count) 중복(여러 스택 보유자)을 제거한다.</li>
     *   <li>확정된 소수의 ID만 member에 PK 조인한다 — 결과 크기를 모르는 N:M 탐색이
     *       아니라 상한이 보장된 lookup이라 실행 계획이 흔들릴 여지가 없다.</li>
     * </ol>
     * 스택 ID와 페이징 값은 전부 숫자라 SQL에 직접 삽입해도 안전하다.
     *
     * @param techStackIds 기술스택 ID 목록 (비어 있지 않아야 함 — 검증은 서비스 레이어 책임)
     */
    public Page<Member> findMembersHavingAnyTechStack(List<Long> techStackIds, Pageable pageable) {
        long fetchDepth = pageable.getOffset() + pageable.getPageSize();

        String perStackTopN = techStackIds.stream()
                .map(stackId -> "(SELECT member_id, project_count FROM member_stack_search" +
                        " WHERE tech_stack_id = " + stackId +
                        " ORDER BY project_count DESC, member_id LIMIT " + fetchDepth + ")")
                .collect(Collectors.joining(" UNION "));

        String sql = "SELECT m.* FROM (" + perStackTopN + ") t" +
                " JOIN member m ON m.member_id = t.member_id" +
                " ORDER BY t.project_count DESC, t.member_id" +
                " LIMIT " + pageable.getPageSize() + " OFFSET " + pageable.getOffset();

        @SuppressWarnings("unchecked")
        List<Member> members = em.createNativeQuery(sql, Member.class).getResultList();

        return PageableExecutionUtils.getPage(members, pageable, () -> countMembersHavingAnyTechStack(techStackIds));
    }

    /**
     * OR 매칭 전체 회원 수. 여러 스택 보유자가 중복 집계되지 않도록 DISTINCT가 필요해
     * 보유 행 수에 비례하는 비용이 든다 — 투영 테이블로도 싸지지 않는 유일한 조각이며,
     * 마지막 페이지 요청 등 {@code PageableExecutionUtils}가 생략 가능한 경우에는 실행되지 않는다.
     */
    private long countMembersHavingAnyTechStack(List<Long> techStackIds) {
        String inList = techStackIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(", "));

        Number count = (Number) em.createNativeQuery(
                        "SELECT COUNT(DISTINCT member_id) FROM member_stack_search WHERE tech_stack_id IN (" + inList + ")")
                .getSingleResult();
        return count.longValue();
    }
}
