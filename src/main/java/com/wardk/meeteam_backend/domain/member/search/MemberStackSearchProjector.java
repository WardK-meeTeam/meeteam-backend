package com.wardk.meeteam_backend.domain.member.search;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * member_stack_search 투영 테이블 동기화 담당.
 * <p>
 * 경로별로 다른 UPDATE 수술 대신 "그 회원의 투영을 지우고 원본에서 다시 만든다"는
 * 멱등 재투영 하나로 통일한다 — 어떤 변경이든 결과 상태는 원본과 일치하므로
 * 갱신 누락·중복 호출에 안전하다.
 * <p>
 * 호출 지점:
 * <ul>
 *   <li>프로필 기술스택 수정 → {@link #rebuildFor}</li>
 *   <li>회원 탈퇴(소프트)·삭제(하드) → {@link #removeFor}</li>
 *   <li>프로젝트 참여/탈퇴(projectCount 변경) → {@code MemberRepository}의
 *       카운터 default 메서드가 투영의 project_count를 같은 트랜잭션에서 미러 갱신
 *       (행 삭제/생성이 없는 변경이라 재투영 없이 UPDATE 하나로 충분)</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class MemberStackSearchProjector {

    private final EntityManager em;

    /**
     * 회원 한 명의 투영을 원본(member + member_tech_stack) 기준으로 다시 만든다.
     * 탈퇴 회원이면 INSERT 대상이 없어 결과적으로 행이 비워진다.
     * <p>
     * 영속성 컨텍스트에 쌓인 스택 변경(clear + 재등록)이 아직 DB에 없을 수 있으므로
     * 먼저 flush하여 네이티브 SELECT가 최신 상태를 읽게 한다.
     */
    public void rebuildFor(Long memberId) {
        em.flush();

        em.createNativeQuery("DELETE FROM member_stack_search WHERE member_id = :memberId")
                .setParameter("memberId", memberId)
                .executeUpdate();

        em.createNativeQuery("""
                        INSERT INTO member_stack_search (tech_stack_id, project_count, member_id)
                        SELECT mts.tech_stack_id, m.project_count, m.member_id
                        FROM member_tech_stack mts
                        JOIN member m ON m.member_id = mts.member_id
                        WHERE m.member_id = :memberId
                          AND m.deleted_at IS NULL
                        """)
                .setParameter("memberId", memberId)
                .executeUpdate();
    }

    /**
     * 회원의 투영 행을 모두 제거한다. 탈퇴(소프트)·삭제(하드) 시 호출 —
     * 투영 테이블에는 검색 가능한 회원만 존재해야 한다.
     */
    public void removeFor(Long memberId) {
        em.createNativeQuery("DELETE FROM member_stack_search WHERE member_id = :memberId")
                .setParameter("memberId", memberId)
                .executeUpdate();
    }
}
