package com.wardk.meeteam_backend.domain.projectlike.repository;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.projectlike.entity.ProjectLike;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectLikeRepository extends JpaRepository<ProjectLike, Long> {

    long deleteByMemberIdAndProjectId(Long memberId, Long projectId);

    Integer countByProjectId(Long projectId);
    Optional<ProjectLike> findByMemberAndProject(Member member, Project project);

    boolean existsByMemberIdAndProjectId(Long memberId, Long projectId);

}
