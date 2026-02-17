package com.wardk.meeteam_backend.domain.application.repository;

import com.wardk.meeteam_backend.domain.application.entity.ProjectApplication;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectApplicationRepository extends JpaRepository<ProjectApplication, Long> {

    boolean existsByProjectAndApplicant(Project project, Member member);

    @Query("""
            SELECT a FROM ProjectApplication a
            JOIN FETCH a.applicant
            JOIN FETCH a.jobPosition jp
            JOIN FETCH jp.jobField
            WHERE a.project.id = :projectId
            AND a.project.isDeleted = false
            AND a.status = 'PENDING'
            ORDER BY a.createdAt DESC
            """)
    List<ProjectApplication> findPendingByProjectIdOrderByCreatedAtDesc(Long projectId);

    @Query("SELECT a FROM ProjectApplication a " +
            "JOIN FETCH a.project p " +
            "WHERE a.applicant.id = :applicantId AND p.isDeleted = false")
    List<ProjectApplication> findAllByApplicantId(Long applicantId);

    @Query("""
            SELECT a FROM ProjectApplication a
            JOIN FETCH a.applicant
            JOIN FETCH a.jobPosition jp
            JOIN FETCH jp.jobField
            WHERE a.project.id = :projectId
            AND a.id = :applicationId
            AND a.project.isDeleted = false
            """)
    Optional<ProjectApplication> findByIdWithApplicantAndJobPosition(Long projectId, Long applicationId);
}
