package com.wardk.meeteam_backend.global.auth.service.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.web.auth.dto.oauth.OAuth2RegisterRequest;

import java.time.LocalDate;
import java.util.List;

/**
 * OAuth2 회원가입 요청을 서비스 레이어로 전달하기 위한 Command 객체.
 * Presentation Layer의 Request DTO를 Service Layer에서 직접 참조하지 않도록 분리합니다.
 */
public record OAuth2RegisterCommand(
        String code,
        String name,
        LocalDate birthDate,
        Gender gender,
        List<MemberJobPositionCommand> jobPositions,
        Integer projectExperienceCount,
        String githubUrl,
        String blogUrl
) {
    public static OAuth2RegisterCommand from(OAuth2RegisterRequest request) {
        List<MemberJobPositionCommand> jobPositionCommands = request.getJobPositions().stream()
                .map(MemberJobPositionCommand::from)
                .toList();

        return new OAuth2RegisterCommand(
                request.getCode(),
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
