package com.wardk.meeteam_backend.web.auth.dto.oauth;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.web.auth.dto.register.SubCategoryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class OAuth2RegisterRequest {
  @Schema(description = "회원가입용 일회용 코드", example = "550e8400-e29b-41d4-a716-446655440000", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotEmpty
  private String code;

  @Schema(description = "이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
  @NotEmpty
  private String name;

  @Schema(description = "나이", example = "27", minimum = "1", maximum = "120")
  @NotNull
  private Integer age;

  @Schema(description = "성별", example = "MALE", allowableValues = {"MALE","FEMALE"})
  private Gender gender;

  @Schema(description = "분야(소분류) 목록")
  private List<SubCategoryDto> subCategories;

  @Schema(description = "기술스택", example = "[\"Java\", \"Spring\", \"MySQL\"]")
  private List<String> skills;

  @Schema(description = "프로젝트 경험 횟수", example = "3", minimum = "0")
  private Integer projectExperienceCount;

  @Schema(description = "GitHub URL", example = "https://github.com/username")
  private String githubUrl;

  @Schema(description = "블로그 URL", example = "https://blog.example.com")
  private String blogUrl;
}
