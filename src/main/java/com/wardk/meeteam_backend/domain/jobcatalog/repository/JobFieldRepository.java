package com.wardk.meeteam_backend.domain.jobcatalog.repository;

import com.wardk.meeteam_backend.domain.jobcatalog.entity.JobField;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface JobFieldRepository extends JpaRepository<JobField, Long> {
    Optional<JobField> findByCode(String code);
}
