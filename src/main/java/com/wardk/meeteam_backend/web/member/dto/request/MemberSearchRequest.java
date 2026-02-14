package com.wardk.meeteam_backend.web.member.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "회원 검색 요청 DTO")
public class MemberSearchRequest {

    @Schema(description = "직무 Id들", example = "BACKEND, FRONTEND, DESIGN, PLANNING, ETC")
    private List<Long> jobFieldIds;

    @Schema(description = "기술 스택 Id들", example = "Java, Spring, ...")
    private List<Long> skillIds;
}
