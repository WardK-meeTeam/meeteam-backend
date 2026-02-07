package com.wardk.meeteam_backend.global.auth.service.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;
import com.wardk.meeteam_backend.web.auth.dto.register.JobPositionRequest;

import java.util.List;

public record RegisterMemberCommand(
        String email,
        String password,
        String name,
        Integer age,
        Gender gender,
        List<JobPositionRequest> jobPositions,
        List<String> skills,
        Integer projectExperienceCount,
        String githubUrl,
        String blogUrl
) {
    public static RegisterMemberCommand from(RegisterRequest request) {
        return new RegisterMemberCommand(
                request.getEmail(),
                request.getPassword(),
                request.getName(),
                request.getAge(),
                request.getGender(),
                request.getJobPositions(),
                request.getSkills(),
                request.getProjectExperienceCount(),
                request.getGithubUrl(),
                request.getBlogUrl()
        );
    }
}