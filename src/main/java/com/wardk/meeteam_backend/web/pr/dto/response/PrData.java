package com.wardk.meeteam_backend.web.pr.dto.response;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.*;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Getter
@AllArgsConstructor
@Builder
public class PrData {

    private String repoFullName;
    private Integer prNumber;

    private String title;
    private String body;
    private String state;
    private boolean isDraft;
    private boolean isMerged;

    private String baseRepo;
    private String baseBranch;

    private String headRepo;
    private String headBranch;
    private String headSha;

    private String authorLogin;

    private Integer additions;
    private Integer deletions;
    private Integer changedFiles;

    private Integer commentsCount;
    private Integer reviewCommentsCount;

    private LocalDateTime closedAt;
    private LocalDateTime mergedAt;

    public static PrData fromWebhook(String repoFullName, int prNumber, JsonNode node) {

        String closedAtStr = node.path("closed_at").asText(null);
        String mergedAtStr = node.path("merged_at").asText(null);

        return PrData.builder()
                .repoFullName(repoFullName)
                .prNumber(prNumber)
                .title(node.path("title").asText(null))
                .body(node.path("body").asText(null))
                .state(node.path("state").asText(null))
                .isDraft(node.path("draft").asBoolean(false))
                .isMerged(node.path("merged").asBoolean(false))
                .baseRepo(node.path("base").path("repo").path("full_name").asText(null))
                .baseBranch(node.path("base").path("ref").asText(null))
                .headRepo(node.path("head").path("repo").path("full_name").asText(null))
                .headBranch(node.path("head").path("ref").asText(null))
                .headSha(node.path("head").path("sha").asText(null))
                .authorLogin(node.path("user").path("login").asText(null))
                .additions(node.path("additions").asInt(0))
                .deletions(node.path("deletions").asInt(0))
                .changedFiles(node.path("changed_files").asInt(0))
                .commentsCount(node.path("comments").asInt(0))
                .reviewCommentsCount(node.path("review_comments").asInt(0))
                .closedAt(closedAtStr == null ? null : OffsetDateTime.parse(closedAtStr, DateTimeFormatter.ISO_DATE_TIME)
                        .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                        .toLocalDateTime())
                .mergedAt(mergedAtStr == null ? null : OffsetDateTime.parse(mergedAtStr, DateTimeFormatter.ISO_DATE_TIME)
                        .atZoneSameInstant(java.time.ZoneId.of("Asia/Seoul"))
                        .toLocalDateTime())
                .build();
    }
}
