package com.wardk.meeteam_backend.domain.applicant.repository;


import com.wardk.meeteam_backend.domain.applicant.entity.RecruitmentState;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.web.recruitmentState.dto.response.ProjectCounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecruitmentStateRepository extends JpaRepository<RecruitmentState, Long> {

    @Query("""
    select new com.wardk.meeteam_backend.web.
    recruitmentState.
    dto.ProjectCounts(sum(pc.currentCount), sum(pc.recruitmentCount)
    )
    from RecruitmentState pc
    where pc.project = :project
    group by pc.project
    """)
    ProjectCounts findTotalCountsByProject(@Param("project") Project project);
}
