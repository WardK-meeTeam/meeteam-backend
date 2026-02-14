package com.wardk.meeteam_backend.domain.jobcatalog.repository;

import com.wardk.meeteam_backend.domain.jobcatalog.entity.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    Optional<TechStack> findByName(String name);
}
