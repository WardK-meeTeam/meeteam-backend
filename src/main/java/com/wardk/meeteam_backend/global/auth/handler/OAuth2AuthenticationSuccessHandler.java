package com.wardk.meeteam_backend.global.auth.handler;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.domain.member.entity.UserRole;
import com.wardk.meeteam_backend.global.auth.dto.CustomOauth2UserDetails;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.domain.member.repository.MemberRepository;
import com.wardk.meeteam_backend.global.config.OAuth2Properties;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.Optional;

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

                // 전체 attributes 로깅 (디버깅용)
                log.info("GitHub attributes 전체: {}", attributes);

                // ProviderId 확실하게 추출
                Object idObj = attributes.get(oAuth2Properties.getProviders().getGithub().getIdAttribute());
                providerId = idObj != null ? String.valueOf(idObj) : null;

                // 이메일 처리
                email = (String) attributes.get(oAuth2Properties.getProviders().getGithub().getEmailAttribute());
                if (email == null || email.trim().isEmpty()) {
                    // GitHub username을 사용
                    String login = (String) attributes.get(oAuth2Properties.getProviders().getGithub().getLoginAttribute());
                    email = login + "@github.local";
                    log.warn("GitHub 사용자 {}의 이메일 정보가 없어 임시 이메일 생성: {}", login, email);
                }

                // Name 처리
                name = (String) attributes.get(oAuth2Properties.getProviders().getGithub().getUserNameAttribute());
                if (name == null || name.trim().isEmpty()) {
                    name = (String) attributes.get(oAuth2Properties.getProviders().getGithub().getLoginAttribute());
                }

                provider = oAuth2Properties.getProviders().getGithub().getName();

                log.info("GitHub OAuth2 User 로그인: email={}, name={}, providerId={}, login={}",
                        email, name, providerId, attributes.get("login"));
            } else if (principal instanceof CustomOauth2UserDetails) {
                CustomOauth2UserDetails oauth2User = (CustomOauth2UserDetails) principal;
                Member member = oauth2User.getMember();
                email = member.getEmail();
                name = member.getRealName();
                log.info("Custom OAuth2 User 로그인 성공: {}", email);

                // GitHub는 이미 Member가 있으므로 바로 JWT 생성하고 리다이렉트
                String accessToken = jwtUtil.createAccessTokenForOAuth2(member);
                String refreshToken = jwtUtil.createRefreshTokenForOAuth2(member);

                setRefreshTokenCookie(response, refreshToken);

                request.getSession().invalidate();

                boolean isNewMember = false; // 임시로 false
                String redirectUrl;
                if (isNewMember) {
                    redirectUrl = oAuth2Properties.getRedirect().getSuccessUrlWithToken(accessToken)
                            + "&memberId=" + member.getId() + "&type=register";
                } else {
                    redirectUrl = oAuth2Properties.getRedirect().getSuccessUrlWithToken(accessToken)
                            + "&memberId=" + member.getId() + "&type=login";
                }

                log.info("GitHub OAuth2 로그인 성공 후 리다이렉트: {}", redirectUrl);
                response.sendRedirect(redirectUrl);
                return; // 여기서 종료

            } else {
                throw new IllegalArgumentException("지원하지 않는 Principal 타입: " + principal.getClass());
            }

            // 사용자 조회 또는 생성 (GitHub의 경우)
            // member null 체크
            Member member = null;
            if (provider != null) {
                member = findOrCreateMember(email, name, providerId, provider);
            }

            // JWT 토큰 생성
            String accessToken;
            String refreshToken;
            if (member != null) {
                // CustomSecurityUserDetails 생성 및 SecurityContext 등록
                CustomSecurityUserDetails userDetails = new CustomSecurityUserDetails(member);
                UsernamePasswordAuthenticationToken authenticationToken =
                        new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authenticationToken);

                accessToken = jwtUtil.createAccessTokenForOAuth2(member);
                refreshToken = jwtUtil.createRefreshTokenForOAuth2(member);
            } else {
                accessToken = jwtUtil.createAccessTokenForOAuth2Email(email, name);
                refreshToken = jwtUtil.createRefreshTokenForOAuth2(member);
            }

            setRefreshTokenCookie(response, refreshToken);

            // 세션 무효화
            request.getSession().invalidate();

            // 리다이렉트 처리 (신규 회원 판단은 임시로 false)
            boolean isNewMember = isNewMemberCreated;
            String redirectUrl;
            if (isNewMember) {
                redirectUrl = oAuth2Properties.getRedirect().getSuccessUrlWithToken(accessToken)
                        + "&memberId=" + member.getId() + "&type=register";
            } else {
                redirectUrl = oAuth2Properties.getRedirect().getSuccessUrlWithToken(accessToken)
                        + "&memberId=" + member.getId() + "&type=login";
            }

            log.info("OAuth2 로그인 성공 후 리다이렉트: {}", redirectUrl);
            response.sendRedirect(redirectUrl);

        } catch (Exception e) {
            log.error("OAuth2 로그인 성공 처리 중 오류 발생", e);
            // 설정에서 가져온 실패 리다이렉트 URL 사용
            String errorRedirectUrl = oAuth2Properties.getRedirect().getFailureUrlWithError("oauth2_success_handler_error");
            response.sendRedirect(errorRedirectUrl);
        }
    }

    /**
     * Refresh Token 쿠키 설정 메서드
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie("refreshToken", refreshToken);
        refreshTokenCookie.setHttpOnly(true);  // JS 접근 불가
        refreshTokenCookie.setSecure(false);   // 개발환경에서는 false (배포시 true)
        refreshTokenCookie.setPath("/");       // 모든 경로에서 유효
        refreshTokenCookie.setMaxAge((int) (jwtUtil.getRefreshExpirationTime() / 1000)); // 30일

        response.addCookie(refreshTokenCookie);
        log.info("Refresh Token 쿠키 설정 완료");
    }

    /**
     * Provider + ProviderId 기반으로 회원 조회 또는 생성
     * 중복 이메일 문제를 해결하기 위해 Provider 정보를 우선으로 사용
     */
    private boolean isNewMemberCreated = false;

    private Member findOrCreateMember(String email, String name, String providerId, String provider) {
        log.info("회원 조회/생성 시작: provider={}, providerId={}, email={}", provider, providerId, email);

        // 필수 정보 검증
        if (providerId == null || provider == null) {
            throw new RuntimeException("Provider 정보가 없습니다: provider=" + provider + ", providerId=" + providerId);
        }

        // 1순위: Provider + ProviderId로 조회 (가장 확실한 방법)
        Optional<Member> existingMember = memberRepository.findByProviderAndProviderId(provider, providerId);
        if (existingMember.isPresent()) {
            log.info("기존 회원 조회 성공: id={}, email={}", existingMember.get().getId(), existingMember.get().getEmail());
            isNewMemberCreated = false; // 기존 회원
            return existingMember.get();
        }

        // 2순위: 동일 provider에서 이메일로 조회 (이메일이 변경된 경우)
        if (email != null && !email.endsWith("@github.local")) {
            Optional<Member> emailMember = memberRepository.findByEmailAndProvider(email, provider);
            if (emailMember.isPresent()) {
                // 기존 회원의 providerId 업데이트
                Member member = emailMember.get();
                member.setProviderId(providerId);
                log.info("기존 회원 providerId 업데이트: id={}, newProviderId={}", member.getId(), providerId);
                isNewMemberCreated = false; // 기존 회원
                return memberRepository.save(member);
            }
        }

        // 3순위: 신규 회원 생성
        try {
            Member newMember = Member.builder()
                    .email(email)
                    .realName(name)
                    .provider(provider)
                    .providerId(providerId)
                    .role(UserRole.USER)
                    .build();

            Member savedMember = memberRepository.save(newMember);
            log.info("새 회원 생성 성공: id={}, email={}", savedMember.getId(), email);
            isNewMemberCreated = true; // 신규 회원
            return savedMember;

        } catch (Exception e) {
            log.error("새로운 OAuth2 사용자 생성 중 오류 발생", e);

            // 동시성 문제 체크
            Optional<Member> concurrentMember = memberRepository.findByProviderAndProviderId(provider, providerId);
            if (concurrentMember.isPresent()) {
                isNewMemberCreated = false; // 기존 회원
                return concurrentMember.get();
            }

            isNewMemberCreated = false; // 기존 회원

            return memberRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("사용자 생성 실패: " + e.getMessage()));
        }
    }


}
