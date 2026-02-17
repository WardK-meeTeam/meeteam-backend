package com.wardk.meeteam_backend.global.auth.service.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.web.auth.dto.register.RegisterRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * 일반 회원가입 요청을 서비스 레이어로 전달하기 위한 Command 객체.
 * 웹 계층과 서비스 계층을 분리합니다.
 */
public record RegisterMemberCommand(
        String email,
        String password,
        String name,
        LocalDate birthDate,
        Gender gender,
        List<MemberJobPositionCommand> jobPositions,
        Integer projectExperienceCount,
        String githubUrl,
        String blogUrl
) {
    public static RegisterMemberCommand from(RegisterRequest request) {
        List<MemberJobPositionCommand> jobPositionCommands = request.getJobPositions().stream()
                .map(MemberJobPositionCommand::from)
                .toList();

        return new RegisterMemberCommand(
                request.getEmail(),
                request.getPassword(),
                request.getName(),
                request.getBirthDate(),
                request.getGender(),
                jobPositionCommands,
                request.getProjectExperienceCount(),
                request.getGithubUrl(),
                request.getBlogUrl()
        );
    }
}
