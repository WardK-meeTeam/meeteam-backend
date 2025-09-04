package com.wardk.meeteam_backend.domain.projectMember.repository;

import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMember;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectMemberRepository extends JpaRepository<ProjectMember, Long> {

    boolean existsByProjectIdAndMemberId(Long projectId, Long memberId);

    Optional<ProjectMember> findByProjectIdAndMemberId(Long projectId, Long memberId);

    void deleteByProjectIdAndMemberId(Long projectId, Long memberId);

    @Query("SELECT pm FROM ProjectMember pm JOIN FETCH pm.project p JOIN FETCH pm.subCategory s WHERE pm.member.id = :memberId")
    List<ProjectMember> findAllByMemberId(Long memberId);
}
