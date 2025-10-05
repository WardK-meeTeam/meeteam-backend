package com.wardk.meeteam_backend.global.auth.filter;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.web.auth.dto.CustomSecurityUserDetails;
import com.wardk.meeteam_backend.global.config.SecurityUrls;
import com.wardk.meeteam_backend.global.util.JwtUtil;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final SecurityUrls securityUrls; // 화이트리스트 경로 관리 클래스

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain) throws ServletException, IOException {

        // 인증 생략 경로
        String uri = request.getRequestURI();
        String method = request.getMethod();
        log.debug("JWT 필터 처리 중인 URI: {}, Method: {}", uri, method);

        // 화이트리스트 경로는 JWT 인증을 건너뛰지만, 토큰이 있으면 사용자 정보를 설정
        if (securityUrls.isWhitelisted(uri, method)) {
            log.debug("화이트리스트 경로로 인증 생략: {} {}", method, uri);

            // 화이트리스트 경로에서도 토큰이 있으면 사용자 정보를 SecurityContext에 설정
            setUserDetailsIfTokenExists(request);

            filterChain.doFilter(request, response);
            return;
        }

        // request에서 Authorization 헤더를 찾음
        String authorization = request.getHeader("Authorization");

        // Authorization 헤더 검증
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            log.error("토큰이 존재하지 않습니다.");
            filterChain.doFilter(request, response);
            return;
        }

        log.info("토큰이 존재합니다");

        // Bearer 부분 제거 후 순수 토큰만 획득
        String token = authorization.split(" ")[1];

        // 토큰 처리 및 사용자 정보 설정
        if (processTokenAndSetUserDetails(token)) {
            filterChain.doFilter(request, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    /**
     * 화이트리스트 경로에서 토큰이 있으면 사용자 정보를 설정
     */
    private void setUserDetailsIfTokenExists(HttpServletRequest request) {
        try {
            String authorization = request.getHeader("Authorization");
            if (authorization != null && authorization.startsWith("Bearer ")) {
                String token = authorization.split(" ")[1];

                // 토큰이 유효하면 사용자 정보 설정
                if (!jwtUtil.isExpired(token)) {
                    processTokenAndSetUserDetails(token);
                    log.debug("화이트리스트 경로에서 토큰 기반 사용자 정보 설정 완료");
                }
            }
        } catch (Exception e) {
            // 화이트리스트 경로에서는 토큰 파싱 실패해도 계속 진행
            log.debug("화이트리스트 경로에서 토큰 파싱 실패, 익명 사용자로 진행: {}", e.getMessage());
        }
    }

    /**
     * 토큰을 처리하고 사용자 정보를 SecurityContext에 설정
     */
    private boolean processTokenAndSetUserDetails(String token) {
        try {
            // 토큰 소멸 시간 검증
            if (jwtUtil.isExpired(token)) {
                log.debug("토큰이 만료됨");
                return false;
            }

            // 토큰에서 username과 memberId 획득
            String email = jwtUtil.getUsername(token);
            Long memberId = jwtUtil.getMemberId(token);

            // Member 객체 생성
            Member member = Member.builder()
                    .email(email)
                    .id(memberId)
                    .build();

            // UserDetails에 회원 정보 객체 담기
            CustomSecurityUserDetails customSecurityUserDetails = new CustomSecurityUserDetails(member);

            // 스프링 시큐리티 인증 토큰 생성
            Authentication authToken = new UsernamePasswordAuthenticationToken(
                    customSecurityUserDetails, null, customSecurityUserDetails.getAuthorities());

            // 세션에 사용자 등록
            SecurityContextHolder.getContext().setAuthentication(authToken);

            log.debug("사용자 정보 SecurityContext에 설정 완료: {}", email);
            return true;

        } catch (Exception e) {
            log.error("토큰 처리 중 오류 발생: {}", e.getMessage());
            return false;
        }
    }
}
