package com.wardk.meeteam_backend.web.member.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "회원 검색 요청 DTO")
public class MemberSearchRequest {

    @Schema(description = "대분류 분야", example = "백엔드, 프론트엔드, 디자인, 기획, 기타")
    private List<String> bigCategories;

    @Schema(description = "기술 스택", example = "Java, Spring, ...")
    private List<String> skillList;
}
