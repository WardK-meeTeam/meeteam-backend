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
    public PrData getPr(String repoFullName, int prNumber, JsonNode webhookPayload) {
        PrData base = PrData.fromWebhook(repoFullName, prNumber, webhookPayload.path("pull_request"));

        String[] parts = repoFullName.split("/");

        JsonNode prNode = githubClient.get("/repos/{owner}/{repo}/pulls/{number}", parts[0], parts[1], prNumber);

        PrData enriched = PrData.fromWebhook(repoFullName, prNumber, prNode);

        return merge(base, enriched);
    }

    @Override
    public List<PrFileData> listFiles(String repoFullName, int prNumber) {
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
                .title(firstNonNull(enriched.getTitle(), base.getTitle()))
                .body(firstNonNull(enriched.getBody(), base.getBody()))
                .state(firstNonNull(enriched.getState(), base.getState()))
                .draft(enriched.isDraft())
                .merged(enriched.isMerged())
                .baseBranch(firstNonNull(enriched.getBaseBranch(), base.getBaseBranch()))
                .headBranch(firstNonNull(enriched.getHeadBranch(), base.getHeadBranch()))
                .headSha(firstNonNull(enriched.getHeadSha(), base.getHeadSha()))
                .authorLogin(firstNonNull(enriched.getAuthorLogin(), base.getAuthorLogin()))
                .additions(firstNonNull(enriched.getAdditions(), base.getAdditions()))
                .deletions(firstNonNull(enriched.getDeletions(), base.getDeletions()))
                .changedFiles(firstNonNull(enriched.getChangedFiles(), base.getChangedFiles()))
                .commentsCount(firstNonNull(enriched.getCommentsCount(), base.getCommentsCount()))
                .reviewCommentsCount(firstNonNull(enriched.getReviewCommentsCount(), base.getReviewCommentsCount()))
                .build();
    }

    private String firstNonNull(String a, String b) {
        return (a != null && !a.isBlank()) ? a : b;
    }

    private Integer firstNonNull(Integer a, Integer b) {
        return (a != null) ? a : b;
    }
}
