package com.wardk.meeteam_backend.domain.recruitment.repository;


import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import com.wardk.meeteam_backend.domain.recruitment.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.web.recruitmentState.dto.response.ProjectCounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecruitmentStateRepository extends JpaRepository<RecruitmentState, Long> {

    @Query("""
    select new com.wardk.meeteam_backend.web.recruitmentState.dto.response.ProjectCounts(
        sum(pc.currentCount), sum(pc.recruitmentCount)
    )
    from RecruitmentState pc
    where pc.project = :project
    group by pc.project
    """)
    ProjectCounts findTotalCountsByProject(@Param("project") Project project);

    @Query("""
    SELECT rs FROM RecruitmentState rs
    JOIN FETCH rs.jobPosition jp
    JOIN FETCH jp.jobField
    WHERE rs.project.id = :projectId
    """)
    List<RecruitmentState> findByProjectIdWithJobPosition(@Param("projectId") Long projectId);

    /**
     * 해당 프로젝트에서 특정 포지션으로 모집 중인 RecruitmentState 조회.
     */
    @Query("""
    SELECT rs FROM RecruitmentState rs
    WHERE rs.project.id = :projectId
    AND rs.jobPosition = :jobPosition
    AND rs.currentCount < rs.recruitmentCount
    """)
    Optional<RecruitmentState> findAvailableByProjectIdAndJobPosition(
            @Param("projectId") Long projectId,
            @Param("jobPosition") JobPosition jobPosition);
}
