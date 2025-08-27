package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional; import java.util.UUID;

public interface ChatThreadRepository extends JpaRepository<ChatThread, Long> {
  Optional<ChatThread> findByPullRequest(PullRequest pullRequest);

  @Query("SELECT ct FROM ChatThread ct WHERE ct.memberId = :memberId ORDER BY ct.createdAt DESC")
  Page<ChatThread> findAllByMemberIdOrderByCreatedAt(Long memberId, Pageable pageable);
}