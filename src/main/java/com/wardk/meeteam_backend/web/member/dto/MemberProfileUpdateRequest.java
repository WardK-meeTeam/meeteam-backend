package com.wardk.meeteam_backend.web.member.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.*;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Schema(description = "회원 프로필 수정 요청")
public class MemberProfileUpdateRequest {

    @NotBlank(message = "이름을 입력해주세요.")
    @Size(max = 50, message = "이름은 50자 이하로 입력해주세요.")
    @Schema(description = "회원 실명", example = "김철수")
    private String name;

    @NotNull(message = "나이를 입력해주세요.")
    @Min(value = 1, message = "나이는 1 이상이어야 합니다.")
    @Max(value = 150, message = "나이는 150 이하여야 합니다.")
    @Schema(description = "회원 나이", example = "25")
    private Integer age;

    @NotNull(message = "성별을 선택해주세요.")
    @Schema(description = "회원 성별", example = "MALE")
    private Gender gender;

    @NotEmpty(message = "관심 분야를 하나 이상 선택해주세요.")
    @Schema(description = "관심 분야 (소분류) 목록",
            example = "[\"웹서버\", \"AI\"]")
    private List<String> subCategories;

    @NotEmpty(message = "기술 스택을 하나 이상 선택해주세요.")
    @Schema(description = "기술 스택 목록",
            example = "[\"Java\", \"Spring Boot\", \"MySQL\"]")
    private List<String> skills;

    @Schema(description = "프로젝트 참여 가능 여부", example = "true")
    private Boolean isParticipating;

    @Size(max = 1000, message = "자기소개는 1000자 이하로 입력해주세요.")
    @Schema(description = "자기소개", example = "백엔드 개발에 관심이 많은 개발자입니다.")
    private String introduction;

}
