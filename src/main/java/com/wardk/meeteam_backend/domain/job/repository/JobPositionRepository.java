package com.wardk.meeteam_backend.domain.job.repository;

import com.wardk.meeteam_backend.domain.job.entity.JobPosition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobPositionRepository extends JpaRepository<JobPosition, Long> {
    Optional<JobPosition> findByCode(String code);
}
