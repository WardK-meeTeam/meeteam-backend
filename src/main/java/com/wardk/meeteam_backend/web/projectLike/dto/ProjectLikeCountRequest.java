package com.wardk.meeteam_backend.web.projectLike.dto;

import lombok.Data;

@Data
public class ProjectLikeCountRequest {

    private Long projectId;

    private Integer likeCount;
}
