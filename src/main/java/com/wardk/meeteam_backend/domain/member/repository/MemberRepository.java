package com.wardk.meeteam_backend.domain.member.repository;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long>, MemberRepositoryCustom {

    /**
     * 참여 프로젝트 수(비정규화 컬럼) 원자적 증가. ProjectMember 생성 시 호출한다.
     * 검색 투영 테이블(member_stack_search)의 project_count도 같은 트랜잭션에서 미러 갱신한다.
     */
    default void increaseProjectCount(Long memberId) {
        applyProjectCountIncrease(memberId);
        shiftStackSearchProjectCount(memberId, 1);
    }

    /**
     * 참여 프로젝트 수(비정규화 컬럼) 원자적 감소. ProjectMember 삭제 시 호출한다.
     * 언더플로 방어가 발동해 member가 갱신되지 않았으면 투영도 건드리지 않는다 — 두 값이 어긋나지 않도록.
     */
    default void decreaseProjectCount(Long memberId) {
        if (applyProjectCountDecrease(memberId) > 0) {
            shiftStackSearchProjectCount(memberId, -1);
        }
    }

    /**
     * {@code @Version} 충돌/lost update 없이 동시성 안전하도록 bulk UPDATE 를 사용한다.
     */
    @Modifying
    @Query("UPDATE Member m SET m.projectCount = m.projectCount + 1 WHERE m.id = :memberId")
    int applyProjectCountIncrease(@Param("memberId") Long memberId);

    /**
     * 언더플로 방어로 {@code projectCount > 0} 조건을 둔다. 반환값 = 갱신된 행 수.
     */
    @Modifying
    @Query("UPDATE Member m SET m.projectCount = m.projectCount - 1 WHERE m.id = :memberId AND m.projectCount > 0")
    int applyProjectCountDecrease(@Param("memberId") Long memberId);

    /**
     * 검색 투영 테이블의 project_count 미러 갱신. 행 구성이 변하지 않는 카운터 변경은
     * 재투영 없이 UPDATE 하나로 충분하다. 탈퇴 회원은 투영에 행이 없어 자연히 no-op.
     */
    @Modifying
    @Query(value = "UPDATE member_stack_search SET project_count = project_count + :delta WHERE member_id = :memberId",
            nativeQuery = true)
    void shiftStackSearchProjectCount(@Param("memberId") Long memberId, @Param("delta") int delta);

    void deleteByEmail(String email);

    Optional<Member> findOptionByEmail(String email);

    Optional<Member> findByEmail(String email);

    Boolean existsByEmail(String email);

    Optional<Member> findByStudentId(String studentId);

    Boolean existsByStudentId(String studentId);

    // ==================== 활성 회원 조회 (로그인용) ====================

    /**
     * 학번으로 활성 회원 조회 (탈퇴하지 않은 회원만)
     */
    Optional<Member> findByStudentIdAndDeletedAtIsNull(String studentId);

    /**
     * 이메일로 활성 회원 조회 (탈퇴하지 않은 회원만)
     */
    Optional<Member> findByEmailAndDeletedAtIsNull(String email);

    /**
     * 학번으로 활성 회원 존재 여부 확인 (탈퇴하지 않은 회원만)
     */
    Boolean existsByStudentIdAndDeletedAtIsNull(String studentId);
}