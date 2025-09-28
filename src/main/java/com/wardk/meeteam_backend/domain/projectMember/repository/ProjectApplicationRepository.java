package com.wardk.meeteam_backend.domain.projectMember.repository;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.domain.projectMember.entity.ProjectMemberApplication;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProjectApplicationRepository extends JpaRepository<ProjectMemberApplication, Long> {

    boolean existsByProjectAndApplicant(Project project, Member member);

    @Query("SELECT a FROM ProjectMemberApplication a " +
            "JOIN FETCH a.applicant " +
            "JOIN FETCH a.subCategory " +
            "WHERE a.project.id = :projectId AND a.project.isDeleted = false")
    List<ProjectMemberApplication> findByProjectId(Long projectId);

    @Query("SELECT a FROM ProjectMemberApplication a " +
            "JOIN FETCH a.project p " +
            "JOIN FETCH a.subCategory " +
            "WHERE a.applicant.id = :applicantId AND p.isDeleted = false")
    List<ProjectMemberApplication> findAllByApplicantId(Long applicantId);

    @Query("SELECT a FROM ProjectMemberApplication a " +
            "JOIN FETCH a.applicant " +
            "JOIN FETCH a.subCategory " +
            "JOIN FETCH a.weekDays " +
            "WHERE a.project.id = :projectId AND a.id = :applicationId AND a.project.isDeleted = false")
    Optional<ProjectMemberApplication> findByIdWithApplicantAndSubCategory(Long projectId, Long applicationId);
}
