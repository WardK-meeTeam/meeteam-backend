package com.wardk.meeteam_backend.domain.pr.service.fetcher;

import com.fasterxml.jackson.databind.JsonNode;
import com.wardk.meeteam_backend.global.github.GithubClient;
import com.wardk.meeteam_backend.web.pr.dto.PrData;
import com.wardk.meeteam_backend.web.pr.dto.PrFileData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class GithubApiFetcher implements PullRequestFetcher {

    private final GithubClient githubClient;

    @Override
    public PrData getPr(String repoFullName, Integer prNumber, JsonNode webhookPayload) {
        PrData base = PrData.fromWebhook(repoFullName, prNumber, webhookPayload.path("pull_request"));

        String[] parts = repoFullName.split("/");

        JsonNode prNode = githubClient.get("/repos/{owner}/{repo}/pulls/{number}", parts[0], parts[1], prNumber);

        PrData enriched = PrData.fromWebhook(repoFullName, prNumber, prNode);

        return merge(base, enriched);
    }

    @Override
    public List<PrFileData> listFiles(String repoFullName, Integer prNumber) {
        String[] parts = repoFullName.split("/");

        JsonNode[] arr = githubClient.getArray("/repos/{owner}/{repo}/pulls/{number}/files", parts[0], parts[1], prNumber);

        List<PrFileData> files = new ArrayList<>();
        if (arr != null) {
            for (JsonNode f : arr) {
                files.add(PrFileData.create(f));
            }
        }

        return files;
    }

    private PrData merge(PrData base, PrData enriched) {
        return PrData.builder()
                .repoFullName(base.getRepoFullName())
                .prNumber(base.getPrNumber())
                .title(firstNonNull(base.getTitle(), enriched.getTitle()))
                .body(firstNonNull(base.getBody(), enriched.getBody()))
                .state(firstNonNull(base.getState(), enriched.getState()))
                .draft(base.isDraft() || enriched.isDraft())
                .merged(base.isMerged() || enriched.isMerged())
                .baseBranch(firstNonNull(base.getBaseBranch(), enriched.getBaseBranch()))
                .headBranch(firstNonNull(base.getHeadBranch(), enriched.getHeadBranch()))
                .headSha(firstNonNull(base.getHeadSha(), enriched.getHeadSha()))
                .authorLogin(firstNonNull(base.getAuthorLogin(), enriched.getAuthorLogin()))
                .additions(firstNonZero(base.getAdditions(), enriched.getAdditions()))
                .deletions(firstNonZero(base.getDeletions(), enriched.getDeletions()))
                .changedFiles(firstNonZero(base.getChangedFiles(), enriched.getChangedFiles()))
                .commentsCount(firstNonZero(base.getCommentsCount(), enriched.getCommentsCount()))
                .reviewCommentsCount(firstNonZero(base.getReviewCommentsCount(), enriched.getReviewCommentsCount()))
                .build();
    }

    private String firstNonNull(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private Integer firstNonZero(Integer a, Integer b) {
        return (a != null && a > 0) ? a : b;
    }
}
