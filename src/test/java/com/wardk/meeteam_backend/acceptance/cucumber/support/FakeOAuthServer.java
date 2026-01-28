package com.wardk.meeteam_backend.acceptance.cucumber.support;

import com.wardk.meeteam_backend.global.auth.repository.OAuthCodeRepository;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthLoginInfo;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthRegisterInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * OAuth2 인증 서버를 시뮬레이션하는 Fake 서버
 * <p>
 * 실제 Google/GitHub OAuth 인증 과정(OAuth2AuthenticationSuccessHandler)을 대체하여
 * Redis에 일회용 코드를 직접 발급합니다.
 * Step에서는 이 코드를 사용해 실제 API를 호출합니다.
 */
@Component
@RequiredArgsConstructor
public class FakeOAuthServer {

    private final OAuthCodeRepository oAuthCodeRepository;

    /**
     * 신규 회원용 OAuth 회원가입 코드 발급
     * OAuth2AuthenticationSuccessHandler의 신규 회원 분기를 시뮬레이션합니다.
     *
     * @param email    OAuth 제공자로부터 받은 이메일
     * @param provider OAuth 제공자 (google, github)
     * @return 일회용 UUID 코드
     */
    public String issueRegisterCode(String email, String provider) {
        String providerId = provider + "-fake-" + System.currentTimeMillis();

        OAuthRegisterInfo registerInfo = new OAuthRegisterInfo(
                email,
                provider,
                providerId,
                "fake-oauth-access-token",
                "register"
        );

        return oAuthCodeRepository.saveRegisterInfo(registerInfo);
    }

    /**
     * 기존 회원용 OAuth 로그인 코드 발급
     * OAuth2AuthenticationSuccessHandler의 기존 회원 분기를 시뮬레이션합니다.
     *
     * @param memberId 기존 회원 ID
     * @return 일회용 UUID 코드
     */
    public String issueLoginCode(Long memberId) {
        OAuthLoginInfo loginInfo = new OAuthLoginInfo(
                memberId,
                "fake-oauth-access-token",
                "login"
        );

        return oAuthCodeRepository.saveLoginInfo(loginInfo);
    }
}
