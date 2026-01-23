package com.wardk.meeteam_backend.domain.project.repository;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import com.wardk.meeteam_backend.web.projectLike.dto.ProjectWithLikeDto;
import jakarta.persistence.Entity;
import jakarta.persistence.LockModeType;
import jakarta.persistence.OneToMany;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> , ProjectRepositoryCustom{

    @Query("SELECT p FROM Project p " +
            "WHERE p.id = :id AND p.isDeleted = false")
    Optional<Project> findActiveById(Long id);

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN FETCH p.recruitments r " +
            "JOIN FETCH r.subCategory " +
            "WHERE p.id = :projectId AND p.isDeleted = false")
    Optional<Project> findByIdWithRecruitment(Long projectId);

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN FETCH p.creator " +
            "LEFT JOIN FETCH p.projectSkills ps " +
            "LEFT JOIN FETCH ps.skill " +
            "WHERE p.isDeleted = false")
    List<Project> findAllWithCreatorAndSkills();

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.members pm " +
            "LEFT JOIN FETCH pm.member m " +
            "WHERE p.id = :projectId AND p.isDeleted = false")
    Optional<Project> findByIdWithMembers(Long projectId);

    // 특정 대분류들을 모집하고 있는 프로젝트 조회 (Slice) - 메인페이지용, recruitmentStatus 파라미터 추가

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.creator " +           // ToOne 관계만
            "LEFT JOIN FETCH p.recruitments r " +    // 컬렉션 1개만
            "LEFT JOIN FETCH r.subCategory sc " +    // ToOne 관계
            "LEFT JOIN FETCH sc.bigCategory bc " +   // ToOne 관계
            "WHERE bc.id = :bigCategoryId " +
            "AND p.recruitmentStatus = :recruitmentStatus " +
            "AND p.isDeleted = false")
    Slice<Project> findRecruitingProjectsByBigCategory(
            @Param("bigCategoryId") Long bigCategoryId,
            @Param("recruitmentStatus") Recruitment recruitmentStatus,
            Pageable pageable
    );

    // 필요시 추가로 skills 조회하는 별도 메서드
    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.projectSkills ps " +
            "JOIN FETCH ps.skill " +
            "WHERE p IN :projects AND p.isDeleted = false")
    List<Project> findProjectsWithSkills(@Param("projects") List<Project> projects);

    @Query("""
       SELECT new com.wardk.meeteam_backend.web.projectLike.dto.ProjectWithLikeDto(p, COUNT(pl))
       FROM Project p
       LEFT JOIN p.projectLikes pl
       WHERE p.id = :projectId AND p.isDeleted = false 
       GROUP BY p
       """)
    Optional<ProjectWithLikeDto> findProjectWithLikeCount(@Param("projectId") Long projectId);

    // 테스트 용도
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            "update Project p set p.createdAt = :ts where p.id = :id"
    )
    void overrideTimestamps(
            @Param("id") Long id,
            @Param("ts") java.time.LocalDateTime ts
    );


    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Project p where p.id = :id AND p.isDeleted = false")
    Optional<Project> findByIdForUpdate(@Param("id") Long id);
}
