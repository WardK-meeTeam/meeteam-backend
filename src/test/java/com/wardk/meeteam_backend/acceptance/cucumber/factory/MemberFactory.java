package com.wardk.meeteam_backend.acceptance.cucumber.factory;

import com.wardk.meeteam_backend.domain.member.entity.Gender;
import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 테스트용 회원 생성 팩토리
 */
@Component
@RequiredArgsConstructor
public class MemberFactory {

    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    private static final String DEFAULT_PASSWORD = "password123";

    /**
     * 기본 회원 생성
     */
    public Member createMember(String name) {
        return createMember(name, name.toLowerCase() + "@meeteam.com", DEFAULT_PASSWORD);
    }

    /**
     * 이메일과 비밀번호를 지정하여 회원 생성
     */
    public Member createMember(String name, String email, String password) {
        return memberRepository.save(Member.builder()
                .realName(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .role(UserRole.USER)
                .isParticipating(true)
                .build());
    }

    /**
     * 상세 정보를 포함한 회원 생성
     */
    public Member createMemberWithDetails(String name, String email, String password, Integer age, Gender gender) {
        return memberRepository.save(Member.builder()
                .realName(name)
                .email(email)
                .password(passwordEncoder.encode(password))
                .age(age)
                .gender(gender)
                .role(UserRole.USER)
                .isParticipating(true)
                .build());
    }

    /**
     * OAuth 회원 생성
     */
    public Member createOAuthMember(String name, String email, String provider, String providerId) {
        return memberRepository.save(Member.builder()
                .realName(name)
                .email(email)
                .password(passwordEncoder.encode("oauth-" + providerId))
                .provider(provider)
                .providerId(providerId)
                .role(UserRole.USER)
                .isParticipating(true)
                .build());
    }

    /**
     * 기본 비밀번호 조회 (로그인 테스트용)
     */
    public String getDefaultPassword() {
        return DEFAULT_PASSWORD;
    }
}
