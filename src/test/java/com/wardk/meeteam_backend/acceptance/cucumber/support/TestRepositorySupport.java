package com.wardk.meeteam_backend.acceptance.cucumber.support;

import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.domain.project.repository.ProjectRepository;
import com.wardk.meeteam_backend.domain.skill.repository.SkillRepository;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Repository 통합 접근
 * 테스트에서 데이터 검증을 위해 Repository에 쉽게 접근할 수 있도록 합니다.
 */
@Getter
@Component
@RequiredArgsConstructor
public class TestRepositorySupport {

    private final MemberRepository member;
    private final ProjectRepository project;
    private final SkillRepository skill;

    // 필요한 Repository가 추가되면 여기에 추가
    // private final ApplicantRepository applicant;
    // private final NotificationRepository notification;
}
