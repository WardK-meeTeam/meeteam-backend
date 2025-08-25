package com.wardk.meeteam_backend.domain.pr.repository;

import com.wardk.meeteam_backend.domain.pr.entity.ProjectRepo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepoRepository extends JpaRepository<ProjectRepo, Long> {

    Optional<ProjectRepo> findByProjectIdAndRepoFullName(Long projectId, String repoFullName);

    Optional<ProjectRepo> findByRepoFullName(String repoFullName);

    boolean existsByProjectIdAndRepoFullName(Long projectId, String repoFullName);
}
