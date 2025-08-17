package com.wardk.meeteam_backend.web.member.dto;

import com.querydsl.core.annotations.QueryProjection;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
public class ReviewResponse {

    private String reviewerName;
    private String comment;


    @QueryProjection
    public ReviewResponse(String reviewerName, String comment) {
        this.reviewerName = reviewerName;
        this.comment = comment;
    }
}
