package com.wardk.meeteam_backend.global.auth.service.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.SubCategoryDto;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder(access = AccessLevel.PRIVATE)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class RegisterMemberCommand {
    private String email;
    private String password;
    private String name;
    private Integer age;
    private Gender gender;
    private LocalDate birth;
    private List<SubCategoryDto> subCategories;
    private List<String> skills;
    private Integer projectExperienceCount;
    private String githubUrl;
    private String blogUrl;

    public static RegisterMemberCommand from(RegisterRequest request) {
        return RegisterMemberCommand.builder()
                .email(request.getEmail())
                .password(request.getPassword())
                .name(request.getName())
                .age(request.getAge())
                .gender(request.getGender())
                .subCategories(request.getSubCategories())
                .skills(request.getSkills())
                .projectExperienceCount(request.getProjectExperienceCount())
                .githubUrl(request.getGithubUrl())
                .blogUrl(request.getBlogUrl())
                .build();
    }
}