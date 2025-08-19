package com.wardk.meeteam_backend.domain.pr.repository;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PullRequestFileRepository extends JpaRepository<PullRequestFile, Long> {
}
