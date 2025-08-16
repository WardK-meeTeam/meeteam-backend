package com.wardk.meeteam_backend.domain.project.repository;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    @Query("SELECT DISTINCT p FROM Project p JOIN FETCH p.recruitments r JOIN FETCH r.subCategory WHERE p.id = :projectId")
    Optional<Project> findByIdWithRecruitment(Long projectId);
}
