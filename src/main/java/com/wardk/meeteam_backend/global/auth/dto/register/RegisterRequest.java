package com.wardk.meeteam_backend.global.auth.dto.register;


import com.wardk.meeteam_backend.domain.member.entity.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RegisterRequest {

    @Schema(description = "이름", example = "홍길동", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String name;

    @Schema(description = "나이", example = "27", minimum = "1", maximum = "120")
    @NotEmpty
    private Integer age;

    @Schema(description = "이메일", example = "meeteam@naver.com", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotEmpty
    private String email;

    @Schema(description = "성별", example = "MALE", allowableValues = {"MALE","FEMALE"})
    private Gender gender;

    @Schema(description = "비밀번호", example = "qwer1234")
    @NotEmpty
    private String password;


    @Schema(description = "분야(소분류) 목록")
    private List<SubCategoryDto> subCategories;

    @Schema(description = "기술스택")
    private List<SkillDto> skills;

    @Schema(description = "자기소개", example = "안녕하세요 , 백엔드 개발자 (Spring)입니다.")
    private String introduce;


}

/*
{"name":"박희운" , "email" : "phu98@naver.com" , "password" : 1234 , "jobtype": WEB }


 */
