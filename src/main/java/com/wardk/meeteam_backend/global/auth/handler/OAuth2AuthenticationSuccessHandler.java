package com.wardk.meeteam_backend.global.auth.handler;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.auth.repository.OAuthCodeRepository;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthLoginInfo;
import com.wardk.meeteam_backend.global.auth.service.dto.OAuthRegisterInfo;
import com.wardk.meeteam_backend.web.auth.dto.CustomOauth2UserDetails;
import com.wardk.meeteam_backend.global.config.OAuth2Properties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final OAuth2Properties oAuth2Properties;
    private final OAuthCodeRepository oAuthCodeRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        CustomOauth2UserDetails userDetails = (CustomOauth2UserDetails) authentication.getPrincipal();

        Member member = userDetails.getMember();
        String redirectUrl;

        if (userDetails.isNewMember()) {
            log.info("신규 회원 가입 절차를 시작합니다. 사용자: {}", member.getEmail());

            OAuthRegisterInfo registerInfo = new OAuthRegisterInfo(
                member.getEmail(),
                member.getProvider(),
                member.getProviderId(),
                userDetails.getOauthAccessToken(),
                "register"
            );
            String code = oAuthCodeRepository.saveRegisterInfo(registerInfo);

            redirectUrl = UriComponentsBuilder.fromUriString(oAuth2Properties.getOauth2RedirectUrl())
                .queryParam("code", code)
                .queryParam("type", "register")
                .build().toUriString();
        } else {
            log.info("기존 회원 로그인을 진행합니다. 사용자: {}", member.getEmail());

            OAuthLoginInfo loginInfo = new OAuthLoginInfo(
                member.getId(),
                userDetails.getOauthAccessToken(),
                "login"
            );
            String code = oAuthCodeRepository.saveLoginInfo(loginInfo);

            redirectUrl = UriComponentsBuilder.fromUriString(oAuth2Properties.getOauth2RedirectUrl())
                .queryParam("code", code)
                .queryParam("type", "login")
                .build().toUriString();
        }

        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }
}
