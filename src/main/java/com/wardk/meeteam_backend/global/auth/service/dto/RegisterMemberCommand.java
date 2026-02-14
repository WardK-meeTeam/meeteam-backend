package com.wardk.meeteam_backend.global.auth.service.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;

import java.time.LocalDate;
import java.util.List;

public record RegisterMemberCommand(
        String email,
        String password,
        String name,
        LocalDate birthDate,
        Gender gender,
        List<Long> jobPositionIds,
        List<Long> techStackIds,
        Integer projectExperienceCount,
        String githubUrl,
        String blogUrl
) {
    public static RegisterMemberCommand from(RegisterRequest request) {
        return new RegisterMemberCommand(
                request.getEmail(),
                request.getPassword(),
                request.getName(),
                request.getBirthDate(),
                request.getGender(),
                request.getJobPositionIds(),
                request.getTechStackIds(),
                request.getProjectExperienceCount(),
                request.getGithubUrl(),
                request.getBlogUrl()
        );
    }
}
