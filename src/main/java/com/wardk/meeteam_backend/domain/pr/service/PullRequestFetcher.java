package com.wardk.meeteam_backend.domain.pr.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.web.pr.dto.PrData;
import com.wardk.meeteam_backend.web.pr.dto.PrFileData;

import java.util.List;

public interface PullRequestFetcher {

    PrData getPr(String repoFullName, Integer prNumber, JsonNode webhookPayload);
    List<PrFileData> listFiles(String repoFullName, Integer prNumber);
}
