package com.wardk.meeteam_backend.domain.member.search;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Objects;

/**
 * 기술스택 팀원 검색 전용 투영 테이블(읽기 모델).
 * <p>
 * member + member_tech_stack에서 파생되는 데이터로, 검색 쿼리 모양에 맞춰 둔다:
 * 활성 회원의 행만 존재하므로 deleted_at 필터가 없고(탈퇴 시 행 삭제),
 * {@code idx_mss_stack_pcount(tech_stack_id, project_count DESC, member_id)}가
 * "스택별 project_count 상위 N명"을 인덱스 순서 그대로 N행만 읽게 한다.
 * <p>
 * 원본과의 동기화는 {@link MemberStackSearchProjector}(스택 수정·탈퇴)와
 * {@code MemberRepository.increaseProjectCount/decreaseProjectCount}(카운터 미러)가 담당한다.
 * 파생 데이터이므로 언제든 원본에서 전체 재구축할 수 있다 (V9 백필 쿼리 참고).
 * <p>
 * 조회·갱신은 전부 네이티브 SQL로 수행하며, 이 엔티티는 테스트 프로필(ddl-auto)의
 * 스키마 생성을 위한 매핑이다.
 */
@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@IdClass(MemberStackSearch.Pk.class)
@Table(name = "member_stack_search", indexes = @Index(
        name = "idx_mss_stack_pcount",
        columnList = "tech_stack_id, project_count DESC, member_id"
))
public class MemberStackSearch {

    @Id
    @Column(name = "tech_stack_id")
    private Long techStackId;

    @Id
    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "project_count", nullable = false)
    private Integer projectCount;

    /**
     * 복합키 (tech_stack_id, member_id).
     */
    public static class Pk implements Serializable {
        private Long techStackId;
        private Long memberId;

        public Pk() {
        }

        public Pk(Long techStackId, Long memberId) {
            this.techStackId = techStackId;
            this.memberId = memberId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pk pk)) return false;
            return Objects.equals(techStackId, pk.techStackId) && Objects.equals(memberId, pk.memberId);
        }

        @Override
        public int hashCode() {
            return Objects.hash(techStackId, memberId);
        }
    }
}
