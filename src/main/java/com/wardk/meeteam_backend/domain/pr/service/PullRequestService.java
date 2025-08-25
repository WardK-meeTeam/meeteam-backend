package com.wardk.meeteam_backend.domain.pr.service;

import com.wardk.meeteam_backend.web.pr.dto.PullRequestResponse;

public interface PullRequestService {

    PullRequestResponse getPullRequest(String repoFullName, Integer prNumber);
}
