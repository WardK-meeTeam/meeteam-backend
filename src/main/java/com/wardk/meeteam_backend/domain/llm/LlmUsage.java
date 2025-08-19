package com.wardk.meeteam_backend.domain.llm;

public record LlmUsage(String modelName, int promptTokens, int completionTokens, String fullText) { }

