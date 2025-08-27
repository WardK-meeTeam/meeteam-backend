package com.wardk.meeteam_backend.web.pr.dto;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequest;
import lombok.Builder;
import lombok.Getter;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class PullRequestResponse {

    private Long id;
    private String repoFullName;
    private Integer prNumber;
    private String title;
    private String body;
    private String state;
    private boolean draft;
    private boolean merged;
    private String baseBranch;
    private String headBranch;
    private String headSha;
    private String authorLogin;
    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;
    private Integer commentsCount;
    private Integer reviewCommentsCount;

    private List<PullRequestFileResponse> files;

    public static PullRequestResponse create(PullRequest pullRequest) {
        return PullRequestResponse.builder()
                .id(pullRequest.getId())
                .repoFullName(pullRequest.getProjectRepo().getRepoFullName())
                .prNumber(pullRequest.getPrNumber())
                .title(pullRequest.getTitle())
                .body(pullRequest.getBody())
                .state(pullRequest.getState())
                .draft(pullRequest.isDraft())
                .merged(pullRequest.isMerged())
                .baseBranch(pullRequest.getBaseBranch())
                .headBranch(pullRequest.getHeadBranch())
                .headSha(pullRequest.getHeadSha())
                .authorLogin(pullRequest.getAuthorLogin())
                .additions(pullRequest.getAdditions())
                .deletions(pullRequest.getDeletions())
                .changedFiles(pullRequest.getChangedFiles())
                .commentsCount(pullRequest.getCommentsCount())
                .reviewCommentsCount(pullRequest.getReviewCommentsCount())
                .files(pullRequest.getFiles().stream()
                        .map(PullRequestFileResponse::create)
                        .collect(Collectors.toList()))
                .build();
    }

    public static List<PullRequestResponse> createList(List<PullRequest> pullRequests) {
        return pullRequests.stream()
                .map(PullRequestResponse::create)
                .collect(Collectors.toList());
    }
}
