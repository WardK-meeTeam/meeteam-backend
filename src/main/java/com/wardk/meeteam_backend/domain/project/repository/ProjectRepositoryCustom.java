package com.wardk.meeteam_backend.domain.project.repository;

import com.wardk.meeteam_backend.domain.project.entity.Project;
import com.wardk.meeteam_backend.web.project.dto.ProjectSearchCondition;
import com.wardk.meeteam_backend.web.project.dto.ProjectSearchResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface ProjectRepositoryCustom {


    Slice<Project> findAllSlicedForSearchAtCondition(ProjectSearchCondition condition, Pageable pageable);
}
