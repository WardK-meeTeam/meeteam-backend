package com.wardk.meeteam_backend.web.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "회원 검색 요청 DTO")
public class MemberSearchRequest {

    @Schema(description = "서브카테고리 이름", example = "안드로이드")
    private String subCategory;

    @Schema(description = "참여 가능 여부", example = "true")
    private Boolean isParticipating;
}
