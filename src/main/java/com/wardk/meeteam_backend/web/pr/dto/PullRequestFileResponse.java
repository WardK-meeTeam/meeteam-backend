package com.wardk.meeteam_backend.web.pr.dto;

import com.wardk.meeteam_backend.domain.pr.entity.PullRequestFile;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PullRequestFileResponse {

    private Long id;
    private String fileName;
    private String status;
    private Integer additions;
    private Integer deletions;
    private Integer changes;
    private String blobSha;
    private String previousFilename;
    private Integer size;
    private String patch;
    private String patchKey;

    public static PullRequestFileResponse create(PullRequestFile file) {
        return PullRequestFileResponse.builder()
                .id(file.getId())
                .fileName(file.getFileName())
                .status(file.getStatus())
                .additions(file.getAdditions())
                .deletions(file.getDeletions())
                .changes(file.getChanges())
                .blobSha(file.getBlobSha())
                .previousFilename(file.getPreviousFilename())
                .size(file.getSize())
                .patch(file.getPatch())
                .patchKey(file.getPatchKey())
                .build();
    }
}
