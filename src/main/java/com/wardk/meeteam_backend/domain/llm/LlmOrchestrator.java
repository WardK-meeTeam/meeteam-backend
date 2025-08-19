package com.wardk.meeteam_backend.domain.llm;

import java.util.UUID;

public interface LlmOrchestrator {
  void generateInitialReview(UUID threadId, UUID prId, String prSummary);
  void generateFollowup(UUID threadId);
  void handleToolCall(UUID threadId, Object toolCall); // TODO: ToolCall 타입 후속 정의
}
