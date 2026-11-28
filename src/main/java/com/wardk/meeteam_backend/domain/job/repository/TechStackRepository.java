package com.wardk.meeteam_backend.domain.job.repository;

import com.wardk.meeteam_backend.domain.job.entity.TechStack;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TechStackRepository extends JpaRepository<TechStack, Long> {
    Optional<TechStack> findByName(String name);
    List<TechStack> findByIdIn(List<Long> teckStackIds);
    List<TechStack> findByNameIn(Collection<String> names);
}
