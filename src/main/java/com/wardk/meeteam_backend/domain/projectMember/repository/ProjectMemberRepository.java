package com.wardk.meeteam_backend.domain.projectMember.repository;

import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {
}
