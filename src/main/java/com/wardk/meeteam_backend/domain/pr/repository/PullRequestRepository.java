package com.wardk.meeteam_backend.domain.pr.repository;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
  Optional<PullRequest> findByRepoFullNameAndPrNumber(String repoFullName, Integer prNumber);

  @Query("SELECT pr FROM PullRequest pr LEFT JOIN FETCH pr.files where pr.repoFullName = :repoFullName AND pr.prNumber = :prNumber")
  Optional<PullRequest> findWithFiles(String repoFullName, Integer prNumber);
}
