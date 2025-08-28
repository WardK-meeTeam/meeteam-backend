package com.wardk.meeteam_backend.web.projectLike.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ToggleLikeResponse {

    private Long projectId;

    private Boolean liked;

    private Integer likeCount;
}
