package com.wardk.meeteam_backend.domain.pr.service;


import com.fasterxml.jackson.databind.JsonNode;

public interface PullRequestIngestionService {
  void handlePullRequest(JsonNode payload, String token);
  void handleMerged(JsonNode payload);
  void handleClosed(JsonNode payload);
}
