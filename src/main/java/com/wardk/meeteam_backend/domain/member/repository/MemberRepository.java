package com.wardk.meeteam_backend.domain.member.repository;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {

    Optional<Member> findOptionByEmail(String email);

    Optional<Member> findByEmail(String email);

    Optional<Member> findByProviderAndProviderId(String provider, String providerId);

    boolean existsByProviderAndProviderId(String provider, String providerId);

    Optional<Member> findByEmailAndProvider(String email, String provider);

    Boolean existsByEmail(String email);

    /**
     * 동적 조건으로 회원 검색 (페이징, 정렬 포함)
     * 회원이 여러 서브카테고리를 가질 수 있으므로 포함 여부로 검색
     *
     * @param subCategory     서브카테고리 이름 (회원의 서브카테고리 중 하나라도 일치하면 포함)
     * @param isParticipating 참여 가능 여부
     * @param pageable        페이징 및 정렬 정보
     * @return 검색된 회원 목록
     */
    @Query("""
            SELECT DISTINCT m
            FROM Member m
            WHERE (:subCategory IS NULL 
                   OR EXISTS (
                       SELECT 1 FROM MemberSubCategory msc 
                       WHERE msc.member = m 
                       AND msc.subCategory.name = :subCategory
                   ))
            AND (:isParticipating IS NULL OR m.isParticipating = :isParticipating)
            """)
    Page<Member> searchMembers(
            @Param("subCategory") String subCategory,
            @Param("isParticipating") Boolean isParticipating,
            Pageable pageable);

}