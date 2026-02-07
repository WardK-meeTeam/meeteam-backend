package com.wardk.meeteam_backend.domain.pr.service.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.web.pr.dto.response.PrData;
import com.wardk.meeteam_backend.web.pr.dto.response.PrFileData;

import java.util.List;

public interface PullRequestFetcher {

    PrData getPr(String repoFullName, int prNumber, JsonNode webhookPayload, String token);
    List<PrFileData> listFiles(String repoFullName, int prNumber, String token);
}
