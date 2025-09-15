package com.wardk.meeteam_backend.global.auth.handler;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.global.auth.dto.CustomOauth2UserDetails;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.config.OAuth2Properties;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final MemberRepository memberRepository;
    private final OAuth2Properties oAuth2Properties; // OAuth2 설정 주입

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {
        try {
            // Principal 타입 확인 후 처리
            Object principal = authentication.getPrincipal();

            String email = null;
            String name = null;
            String providerId = null;
            String provider = null;

            if (principal instanceof DefaultOidcUser) {
                // Google 로그인 (OIDC)
                DefaultOidcUser oidcUser = (DefaultOidcUser) principal;
                email = oidcUser.getAttribute(oAuth2Properties.getProviders().getGoogle().getEmailAttribute());
                name = oidcUser.getAttribute(oAuth2Properties.getProviders().getGoogle().getUserNameAttribute());
                providerId = oidcUser.getSubject();
                provider = oAuth2Properties.getProviders().getGoogle().getName();
                log.info("Google OIDC User 로그인 성공: {}", email);
            } else if (principal instanceof DefaultOAuth2User) {
                // GitHub 로그인
                DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
                Map<String, Object> attributes = oauth2User.getAttributes();
                email = (String) attributes.get(oAuth2Properties.getProviders().getGithub().getEmailAttribute());
                name = (String) attributes.get(oAuth2Properties.getProviders().getGithub().getUserNameAttribute());
                if (name == null || name.trim().isEmpty()) {
                    name = (String) attributes.get(oAuth2Properties.getProviders().getGithub().getLoginAttribute());
                }
                providerId = String.valueOf(attributes.get(oAuth2Properties.getProviders().getGithub().getIdAttribute()));
                provider = oAuth2Properties.getProviders().getGithub().getName();
                log.info("GitHub OAuth2 User 로그인 성공: {}", email);
            } else if (principal instanceof CustomOauth2UserDetails) {
                CustomOauth2UserDetails oauth2User = (CustomOauth2UserDetails) principal;
                Member member = oauth2User.getMember();
                email = member.getEmail();
                name = member.getRealName();
                log.info("Custom OAuth2 User 로그인 성공: {}", email);
            } else {
                throw new IllegalArgumentException("지원하지 않는 Principal 타입: " + principal.getClass());
            }

            // 사용자 조회 또는 생성 (GitHub의 경우)
            Member member = null;
            if (provider != null) {
                member = findOrCreateMember(email, name, providerId, provider);
            }

            // JWT 토큰 생성 (이메일 기반)
            String accessToken;
            if (member != null) {
                accessToken = jwtUtil.createAccessTokenForOAuth2(member);
            } else {
                accessToken = jwtUtil.createAccessTokenForOAuth2Email(email, name);
            }

            // 세션 무효화
            request.getSession().invalidate();

            // 설정에서 가져온 리다이렉트 URL 사용
            String redirectUrl = oAuth2Properties.getRedirect().getSuccessUrlWithToken(accessToken);
            log.info("OAuth2 로그인 성공 후 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생", e);
            // 설정에서 가져온 실패 리다이렉트 URL 사용
            String errorRedirectUrl = oAuth2Properties.getRedirect().getFailureUrlWithError("oauth2_success_handler_error");
            response.sendRedirect(errorRedirectUrl);
        }
    }

    private Member findOrCreateMember(String email, String name, String providerId, String provider) {
        return memberRepository.findByEmail(email)
            .orElse(
                memberRepository.save(
                    Member.builder()
                        .email(email)
                        .realName(name)
                        .provider(provider)
                        .providerId(providerId)
                        .role(UserRole.USER)
                        .build()
                )
            );
    }

}
