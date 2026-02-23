package com.wardk.meeteam_backend.web.auth.dto.register;


import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.wardk.meeteam_backend.domain.member.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@JsonPropertyOrder({"email", "password", "name", "birthDate", "gender", "jobPositions", "projectExperienceCount", "githubUrl", "blogUrl"})
public class RegisterRequest {

    // === 필수 입력 (인증 정보) ===
    @Schema(description = "이메일", example = "meeteam@naver.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이메일을 입력해주세요")
    @Email(message = "올바른 이메일 형식이 아닙니다")
    private String email;

    @Schema(description = "비밀번호", example = "qwer1234", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호를 입력해주세요")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다")
    private String password;

    // === 필수 입력 (기본 정보) ===
    @Schema(description = "이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "이름을 입력해주세요")
    private String name;

    @Schema(description = "생년월일", example = "1998-03-15", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "생년월일을 입력해주세요")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "MALE", allowableValues = {"MALE","FEMALE"})
    @NotNull(message = "성별을 선택해주세요")
    private Gender gender;


    @NotEmpty(message = "관심분야를 최소 1개 이상 선택해주세요")
    @Valid
    private List<MemberJobPositionRequest> jobPositions = new ArrayList<>();

    @Schema(description = "프로젝트 경험 횟수", example = "3", minimum = "0", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "프로젝트 경험 횟수를 입력해주세요")
    private Integer projectExperienceCount;

    // === 선택 입력 ===
    @Schema(description = "GitHub URL", example = "https://github.com/username")
    private String githubUrl;

    @Schema(description = "블로그 URL", example = "https://blog.example.com")
    private String blogUrl;

}
