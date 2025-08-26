package com.wardk.meeteam_backend.domain.project.repository;

import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.project.entity.ProjectSkill;
import com.wardk.meeteam_backend.domain.project.entity.Recruitment;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN FETCH p.recruitments r " +
            "JOIN FETCH r.subCategory " +
            "WHERE p.id = :projectId")
    Optional<Project> findByIdWithRecruitment(Long projectId);

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN FETCH p.creator " +
            "LEFT JOIN FETCH p.projectSkills ps " +
            "LEFT JOIN FETCH ps.skill")
    List<Project> findAllWithCreatorAndSkills();

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN FETCH p.members pm " +
            "JOIN FETCH pm.member m " +
            "WHERE p.id = :projectId")
    Optional<Project> findByIdWithMembers(Long projectId);

    @Query("SELECT ps FROM ProjectSkill ps JOIN FETCH ps.skill WHERE ps.project.id = :projectId")
    List<ProjectSkill> findSkillsByProjectId(Long projectId);

    @Query("SELECT r FROM ProjectCategoryApplication r JOIN FETCH r.subCategory WHERE r.project.id = :projectId")
    List<ProjectCategoryApplication> findRecruitmentsByProjectId(Long projectId);


    // 특정 대분류들을 모집하고 있는 프로젝트 조회 (Slice) - 메인페이지용, recruitmentStatus 파라미터 추가

    @Query("SELECT DISTINCT p FROM Project p " +
            "JOIN FETCH p.creator " +
            "JOIN FETCH p.recruitments r " +
            "JOIN FETCH r.subCategory sc " +
            "JOIN FETCH sc.bigCategory bc " +
            "WHERE bc.id IN :bigCategoryIds " +
            "AND p.recruitmentStatus = :recruitmentStatus " +  // Enum 파라미터 추가
            "AND p.isDeleted = false")
    Slice<Project> findRecruitingProjectsByBigCategories(
            @Param("bigCategoryIds") List<Long> bigCategoryIds,
            @Param("recruitmentStatus") Recruitment recruitmentStatus,  // Enum 파라미터 추가
            Pageable pageable
    );

    // 필요시 추가로 skills 조회하는 별도 메서드
    @Query("SELECT p FROM Project p " +
            "JOIN FETCH p.projectSkills ps " +
            "JOIN FETCH ps.skill " +
            "WHERE p IN :projects")
    List<Project> findProjectsWithSkills(@Param("projects") List<Project> projects);

}
