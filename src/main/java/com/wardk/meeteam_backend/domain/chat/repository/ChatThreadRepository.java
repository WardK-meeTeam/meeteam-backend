package com.wardk.meeteam_backend.domain.chat.repository;

import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional; import java.util.UUID;

public interface ChatThreadRepository extends JpaRepository<ChatThread, UUID> {
  Optional<ChatThread> findByPullRequestId(UUID prId);
}
