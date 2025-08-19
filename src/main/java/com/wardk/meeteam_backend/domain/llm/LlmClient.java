package com.wardk.meeteam_backend.domain.llm;


import java.util.function.Consumer;

public interface LlmClient {
  void stream(LlmRequest request, Consumer<String> onChunk, Consumer<LlmUsage> onDone);
}

