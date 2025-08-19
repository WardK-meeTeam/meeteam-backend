package com.wardk.meeteam_backend.web.pr.controller;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.repository.PullRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class PullRequestController {

  private final PullRequestRepository pullRequestRepository;

  @GetMapping("/{repoFullName}/{prNumber}")
  public ResponseEntity<PullRequest> get(
      @PathVariable String repoFullName, @PathVariable Integer prNumber) {
    Optional<PullRequest> pr = pullRequestRepository.findByRepoFullNameAndPrNumber(repoFullName, prNumber);
    return pr.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
  }

  // TODO: 필요 시 보강 조회/관리용 엔드포인트 추가
}
