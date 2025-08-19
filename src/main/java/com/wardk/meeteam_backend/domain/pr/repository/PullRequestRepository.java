package com.wardk.meeteam_backend.domain.pr.repository;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
  Optional<PullRequest> findByRepoFullNameAndPrNumber(String repoFullName, Integer prNumber);
}
