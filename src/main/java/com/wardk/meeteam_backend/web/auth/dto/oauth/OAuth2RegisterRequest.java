package com.wardk.meeteam_backend.web.auth.dto.oauth;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.wardk.meeteam_backend.domain.job.JobPosition;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"code", "name", "birthDate", "gender", "jobPositions", "skills", "projectExperienceCount", "githubUrl", "blogUrl"})
public class OAuth2RegisterRequest {

  // === 필수 입력 (OAuth 인증 정보) ===
  @Schema(description = "회원가입용 일회용 코드", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "인증 코드를 입력해주세요")
  private String code;

  // === 필수 입력 (기본 정보) ===
  @Schema(description = "이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotBlank(message = "이름을 입력해주세요")
  private String name;

  @Schema(description = "생년월일", example = "2000-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "생년월일을 입력해주세요")
  private LocalDate birthDate;

  @Schema(description = "성별", example = "MALE", allowableValues = {"MALE","FEMALE"})
  @NotNull(message = "성별을 선택해주세요")
  private Gender gender;

  @Schema(description = "관심분야 (직무 포지션 목록)", example = "[\"WEB_SERVER\", \"WEB_FRONTEND\"]", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotEmpty(message = "관심분야를 최소 1개 이상 선택해주세요")
  private List<JobPosition> jobPositions;

  @Schema(description = "프로젝트 경험 횟수", example = "3", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotNull(message = "프로젝트 경험 횟수를 입력해주세요")
  private Integer projectExperienceCount;

  // === 선택 입력 ===
  @Schema(description = "기술스택", example = "[\"Java\", \"Spring\", \"MySQL\"]")
  private List<String> skills;

  @Schema(description = "GitHub URL", example = "https://github.com/username")
  private String githubUrl;

  @Schema(description = "블로그 URL", example = "https://blog.example.com")
  private String blogUrl;
}
