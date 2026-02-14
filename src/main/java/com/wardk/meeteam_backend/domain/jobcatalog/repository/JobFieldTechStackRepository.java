package com.wardk.meeteam_backend.domain.jobcatalog.repository;

import com.wardk.meeteam_backend.domain.jobcatalog.entity.JobFieldTechStack;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobFieldTechStackRepository extends JpaRepository<JobFieldTechStack, Long> {
    boolean existsByJobFieldIdAndTechStackId(Long jobFieldId, Long techStackId);
}
