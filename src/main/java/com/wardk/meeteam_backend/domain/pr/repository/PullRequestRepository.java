package com.wardk.meeteam_backend.domain.pr.repository;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PullRequestRepository extends JpaRepository<PullRequest, Long> {
  Optional<PullRequest> findByProjectRepoIdAndPrNumber(Long projectRepoId, Integer prNumber);

  @Query("SELECT pr FROM PullRequest pr JOIN FETCH pr.projectRepo repo LEFT JOIN FETCH pr.files WHERE repo.repoFullName = :repoFullName AND pr.prNumber = :prNumber")
  Optional<PullRequest> findWithFiles(String repoFullName, Integer prNumber);

  @Query("SELECT pr FROM PullRequest pr JOIN FETCH pr.projectRepo repo LEFT JOIN FETCH pr.files WHERE repo.project.id = :projectId")
  List<PullRequest> findAllByProjectIdWithFiles(Long projectId);
}
