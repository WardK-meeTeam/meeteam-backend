package com.wardk.meeteam_backend.global.auth.service.dto;

import com.wardk.meeteam_backend.domain.member.entity.Gender;

import java.time.LocalDate;
import java.util.List;

/**
 * 세종대 포털 인증 후 회원가입 요청을 서비스 레이어로 전달하기 위한 Command 객체.
 */
public record SejongRegisterCommand(
        String studentId,
        String password,
        String name,
        LocalDate birthDate,
        Gender gender,
        List<MemberJobPositionCommand> jobPositions,
        Integer projectExperienceCount,
        String githubUrl,
        String blogUrl
) {
}