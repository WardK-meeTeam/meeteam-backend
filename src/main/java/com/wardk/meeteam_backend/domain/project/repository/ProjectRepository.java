package com.wardk.meeteam_backend.domain.project.repository;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectRepository extends JpaRepository<Project, Long> {
}
