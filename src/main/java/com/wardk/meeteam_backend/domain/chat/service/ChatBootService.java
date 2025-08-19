package com.wardk.meeteam_backend.domain.chat.service;


import com.wardk.meeteam_backend.domain.chat.entity.ChatThread;
import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;

import java.util.UUID;

public interface ChatBootService {
  ChatThread createThreadWithIntro(PullRequest pr, String summary);
  void enqueueFirstAssistantMessage(UUID threadId, UUID prId, String summary);
}
