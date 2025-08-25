package com.wardk.meeteam_backend.web.pr.dto;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
public class PrFileData {

    private String fileName;
    private String status;
    private Integer additions;
    private Integer deletions;
    private Integer changes;
    private String blobSha;
    private String blobUrl;
    private String previousFileName;
    private Integer size;
    private String patch;

    public PrFileData(String fileName, String status, Integer additions, Integer deletions, Integer changes, String blobSha, String previousFileName, Integer size, String patch) {
        this.fileName = fileName;
        this.status = status;
        this.additions = additions;
        this.deletions = deletions;
        this.changes = changes;
        this.blobSha = blobSha;
        this.previousFileName = previousFileName;
        this.size = size;
        this.patch = patch;
    }

    public static PrFileData create(JsonNode f) {
        return PrFileData.builder()
                .fileName(f.path("filename").asText(null))
                .status(f.path("status").asText(null))
                .additions(f.path("additions").asInt(0))
                .deletions(f.path("deletions").asInt(0))
                .changes(f.path("changes").asInt(0))
                .blobSha(f.path("sha").asText(null))
                .blobUrl(f.path("blob_url").asText(null))
                .previousFileName(f.path("previous_filename").asText(null))
                .size(f.path("size").asInt(0))
                .patch(f.path("patch").asText(null))
                .build();
    }
}
