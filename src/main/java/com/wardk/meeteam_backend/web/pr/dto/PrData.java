package com.wardk.meeteam_backend.web.pr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

@Getter
@AllArgsConstructor
@Builder
public class PrData {

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

    public static PrData fromWebhook(String repoFullName, int prNumber, JsonNode node) {
        return PrData.builder()
                .repoFullName(repoFullName)
                .prNumber(prNumber)
                .title(node.path("title").asText(null))
                .body(node.path("body").asText(null))
                .state(node.path("state").asText(null))
                .draft(node.path("draft").asBoolean(false))
                .merged(node.path("merged").asBoolean(false))
                .baseBranch(node.path("base").path("ref").asText(null))
                .headBranch(node.path("head").path("ref").asText(null))
                .headSha(node.path("head").path("sha").asText(null))
                .authorLogin(node.path("user").path("login").asText(null))
                .additions(node.path("additions").asInt(0))
                .deletions(node.path("deletions").asInt(0))
                .changedFiles(node.path("changed_files").asInt(0))
                .commentsCount(node.path("comments").asInt(0))
                .reviewCommentsCount(node.path("review_comments").asInt(0))
                .build();
    }
}
