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
     * 대분류와 기술스택으로 회원 검색 (조건이 있을 때만)
     */
    @Query(value = """
            SELECT DISTINCT m.*
            FROM member m
            WHERE  EXISTS (
                SELECT 1 
                FROM member_sub_category ms
                JOIN sub_category sc ON sc.sub_category_id = ms.sub_category_id
                JOIN big_category bc ON bc.big_category_id = sc.big_category_id
                WHERE ms.member_id = m.member_id 
                  AND bc.big_category IN (:bigCategories)
            )
            AND EXISTS (
                SELECT 1 
                FROM member_skill msk
                JOIN skill s ON s.skill_id = msk.skill_id
                WHERE msk.member_id = m.member_id 
                  AND s.skill_name IN (:skillList)
            )
            ORDER BY m.temperature DESC
            """,
            nativeQuery = true)
    Page<Member> findByBigCategoriesAndSkills(
            @Param("bigCategories") List<String> bigCategories,
            @Param("skillList") List<String> skillList,
            Pageable pageable);

    /**
     * 대분류로만 회원 검색
     */
    @Query(value = """
            SELECT DISTINCT m.*
            FROM member m
            WHERE EXISTS (
                SELECT 1 
                FROM member_sub_category ms
                JOIN sub_category sc ON sc.sub_category_id = ms.sub_category_id
                JOIN big_category bc ON bc.big_category_id = sc.big_category_id
                WHERE ms.member_id = m.member_id 
                  AND bc.big_category IN (:bigCategories)
            )
            ORDER BY m.temperature DESC
            """,
            nativeQuery = true)
    Page<Member> findByBigCategories(
            @Param("bigCategories") List<String> bigCategories,
            Pageable pageable);

    /**
     * 기술스택으로만 회원 검색
     */
    @Query(value = """
            SELECT DISTINCT m.*
            FROM member m
            WHERE EXISTS (
                SELECT 1 
                FROM member_skill msk
                JOIN skill s ON s.skill_id = msk.skill_id
                WHERE msk.member_id = m.member_id 
                  AND s.skill_name IN (:skillList)
            )
            ORDER BY m.temperature DESC
            """,
            nativeQuery = true)
    Page<Member> findBySkills(
            @Param("skillList") List<String> skillList,
            Pageable pageable);
}