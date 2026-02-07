package com.wardk.meeteam_backend.web.member.dto.response;

import com.querydsl.core.annotations.QueryProjection;
import lombok.Data;

@Data
public class MemberReviewResponse {

    private String reviewerName;
    private String comment;


    @QueryProjection
    public MemberReviewResponse(String reviewerName, String comment) {
        this.reviewerName = reviewerName;
        this.comment = comment;
    }
}
