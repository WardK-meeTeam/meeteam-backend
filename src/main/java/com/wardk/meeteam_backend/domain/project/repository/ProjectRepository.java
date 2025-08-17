package com.wardk.meeteam_backend.domain.project.repository;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

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

    @Query("SELECT DISTINCT p FROM Project p " +
            "LEFT JOIN FETCH p.projectSkills ps " +
            "LEFT JOIN FETCH ps.skill " +
            "LEFT JOIN FETCH p.recruitments r " +
            "LEFT JOIN FETCH r.subCategory " +
            "WHERE p.id = :projectId")
    Optional<Project> findByIdWithSkillsAndRecruitments(Long projectId);
}
