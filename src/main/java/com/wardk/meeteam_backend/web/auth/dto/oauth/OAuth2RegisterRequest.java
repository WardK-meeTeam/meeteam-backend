package com.wardk.meeteam_backend.web.auth.dto.oauth;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.web.auth.dto.register.SkillDto;
import com.wardk.meeteam_backend.web.auth.dto.register.SubCategoryDto;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
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
  @NotEmpty
  private Integer age;

  @Schema(description = "성별", example = "MALE", allowableValues = {"MALE","FEMALE"})
  private Gender gender;

  @Schema(description = "분야(소분류) 목록")
  private List<SubCategoryDto> subCategories;

  @Schema(description = "기술스택")
  private List<SkillDto> skills;
}
