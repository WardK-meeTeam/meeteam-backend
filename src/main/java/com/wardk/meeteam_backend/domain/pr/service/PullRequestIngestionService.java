package com.wardk.meeteam_backend.domain.pr.service;


import com.fasterxml.jackson.databind.JsonNode;

public interface PullRequestIngestionService {
  void handlePullRequest(JsonNode payload); // TODO: 구현체는 후속 이슈에서 작성
}
