package com.wardk.meeteam_backend.domain.projectMember.repository;

import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.member WHERE pm.project.id = :projectId")
    List<ProjectMember> findAllByProjectIdWithMember(@Param("projectId") Long projectId);

    boolean existsByProjectIdAndMemberId(Long projectId, Long memberId);

    Optional<ProjectMember> findByProjectIdAndMemberId(Long projectId, Long memberId);

    void deleteByProjectIdAndMemberId(Long projectId, Long memberId);
}
