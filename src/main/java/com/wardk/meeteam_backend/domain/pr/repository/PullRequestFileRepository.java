package com.wardk.meeteam_backend.domain.pr.repository;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PullRequestFileRepository extends JpaRepository<PullRequestFile, Long> {

    // PR에 속한 모든 파일 조회
    List<PullRequestFile> findByPullRequest(PullRequest pullRequest);
}
