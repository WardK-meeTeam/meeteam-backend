package com.wardk.meeteam_backend.web.member.dto.request;

import com.wardk.meeteam_backend.domain.job.JobField;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.List;

@Data
@Schema(description = "회원 검색 요청 DTO")
public class MemberSearchRequest {

    @Schema(description = "대분류 분야 (ENUM 값)", example = "BACKEND, FRONTEND, DESIGN, PLANNING, ETC")
    private List<JobField> jobFields;

    @Schema(description = "기술 스택", example = "Java, Spring, ...")
    private List<String> skills;
}
