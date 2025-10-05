package com.wardk.meeteam_backend.global.auth.handler;

import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.web.auth.dto.CustomOauth2UserDetails;
import com.wardk.meeteam_backend.global.config.OAuth2Properties;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.http.Cookie;
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

    private final JwtUtil jwtUtil;
    private final OAuth2Properties oAuth2Properties; // OAuth2 설정 주입

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException {

        // CustomOAuth2UserService 에서 전달해준 CustomOauth2UserDetails 객체를 가져옴
        CustomOauth2UserDetails userDetails = (CustomOauth2UserDetails) authentication.getPrincipal();

        Member member = userDetails.getMember();
        String redirectUrl;

        // isNewMember 플래그를 통해 신규/기존 회원 분기 처리
        if (userDetails.isNewMember()) {
            log.info("신규 회원 가입 절차를 시작합니다. 사용자: {}", member.getEmail());
            // 임시 가입 토큰 생성
            String signupToken = jwtUtil.createOAuth2SignupToken(member);

            // 추가 정보 입력 페이지로 리다이렉트
            redirectUrl = UriComponentsBuilder.fromUriString(oAuth2Properties.getRedirect().getOauth2SignupUrl())
                .queryParam("token", signupToken)
                .build().toUriString();
        } else {
            log.info("기존 회원 로그인을 진행합니다. 사용자: {}", member.getEmail());
            // 로그인용 Access/Refresh 토큰 생성
            String accessToken = jwtUtil.createAccessToken(userDetails.getMember());
            String refreshToken = jwtUtil.createRefreshToken(userDetails.getMember());

            // Refresh Token은 쿠키에 담아 전달
            setRefreshTokenCookie(response, refreshToken);

            // Access Token은 쿼리 파라미터로 프론트엔드에 전달
            redirectUrl = UriComponentsBuilder.fromUriString(oAuth2Properties.getRedirect().getLoginSuccessUrl())
                .queryParam("accessToken", accessToken)
                .build().toUriString();
        }
        // 생성된 URL로 리다이렉트
        getRedirectStrategy().sendRedirect(request, response, redirectUrl);
    }

    /**
     * Refresh Token 쿠키 설정 메서드
     */
    private void setRefreshTokenCookie(HttpServletResponse response, String refreshToken) {
        Cookie refreshTokenCookie = new Cookie(JwtUtil.REFRESH_COOKIE_NAME, refreshToken);
        refreshTokenCookie.setHttpOnly(true);  // JS 접근 불가
        refreshTokenCookie.setSecure(false);   // 개발환경에서는 false (배포시 true)
        refreshTokenCookie.setPath("/");       // 모든 경로에서 유효
        refreshTokenCookie.setMaxAge((int) (jwtUtil.getRefreshExpirationTime() / 1000)); // 30일

        response.addCookie(refreshTokenCookie);
        log.info("Refresh Token 쿠키 설정 완료");
    }
}
