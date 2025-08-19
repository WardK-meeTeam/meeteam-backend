package com.wardk.meeteam_backend.domain.llm;

import java.util.List; import java.util.Map;

public record LlmRequest(List<ChatTurn> turns, Map<String, String> context) {
  public record ChatTurn(String role, String content) { } // system/user/assistant/tool
}
