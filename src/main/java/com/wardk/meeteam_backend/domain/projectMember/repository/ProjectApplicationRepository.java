package com.wardk.meeteam_backend.domain.projectMember.repository;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProjectApplicationRepository extends JpaRepository<ProjectMemberApplication, Long> {

    boolean existsByProjectAndApplicant(Project project, Member member);
}
