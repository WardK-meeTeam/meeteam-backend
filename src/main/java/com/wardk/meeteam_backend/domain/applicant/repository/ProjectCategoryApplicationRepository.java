package com.wardk.meeteam_backend.domain.applicant.repository;


import com.wardk.meeteam_backend.domain.applicant.entity.ProjectCategoryApplication;
import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.web.projectCategoryApplication.dto.ProjectCounts;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ProjectCategoryApplicationRepository extends JpaRepository<ProjectCategoryApplication, Long> {

    @Query("""
    select new com.wardk.meeteam_backend.web.
    projectCategoryApplication.
    dto.ProjectCounts(sum(pc.currentCount), sum(pc.recruitmentCount)
    )
    from ProjectCategoryApplication pc
    where pc.project =: project
    group by pc.project
    """)
    ProjectCounts findTotalCountsByProject(@Param("project") Project project);
}
