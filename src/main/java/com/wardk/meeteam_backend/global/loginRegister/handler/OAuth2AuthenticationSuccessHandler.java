package com.wardk.meeteam_backend.global.loginRegister.handler;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.global.loginRegister.dto.CustomOauth2UserDetails;
import com.wardk.meeteam_backend.global.loginRegister.repository.MemberRepository;
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
                email = oidcUser.getEmail();
                name = oidcUser.getFullName();
                providerId = oidcUser.getSubject();
                provider = "google";
                log.info("Google OIDC User 로그인 성공: {}", email);
            }else if (principal instanceof DefaultOAuth2User) {
                // GitHub 로그인
                DefaultOAuth2User oauth2User = (DefaultOAuth2User) principal;
                Map<String, Object> attributes = oauth2User.getAttributes();
                email = (String) attributes.get("email");
                name = (String) attributes.get("name");
                if (name == null || name.trim().isEmpty()) {
                    name = (String) attributes.get("login");
                }
                providerId = String.valueOf(attributes.get("id"));
                provider = "github";
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

            // 프론트엔드로 리다이렉트
            String redirectUrl = "http://localhost:3000/oauth2/redirect?token=" + accessToken;
            log.info("OAuth2 로그인 성공 후 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생", e);
            response.sendRedirect("http://localhost:3000/login?error=oauth2_success_handler_error");
        }
    }

    private Member findOrCreateMember(String email, String name, String providerId, String provider) {
        Member findMember = memberRepository.findByEmail(email);

        if (findMember == null) {
            Member member = Member.builder()
                    .email(email)
                    .realName(name)
                    .provider(provider)
                    .providerId(providerId)
                    .role(UserRole.USER)
                    .build();
            return memberRepository.save(member);
        }

        return findMember;
    }

}
