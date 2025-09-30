package com.wardk.meeteam_backend.global.auth.filter;


import com.wardk.meeteam_backend.domain.member.entity.Member;
import com.wardk.meeteam_backend.global.auth.dto.CustomSecurityUserDetails;
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
import org.springframework.util.AntPathMatcher;
import org.springframework.util.PathMatcher;
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

        // 화이트리스트 경로는 JWT 인증을 건너뛰고 바로 다음 필터로 진행
        if (securityUrls.isWhitelisted(uri, method)) {
            log.debug("화이트리스트 경로로 인증 생략: {} {}", method, uri);
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

        //토큰 소멸 시간 검증
        if (jwtUtil.isExpired(token)) {

            System.out.println("token expired");
            filterChain.doFilter(request, response);

            //조건이 해당되면 메소드 종료 (필수)
            return;
        }

        //토큰에서 username과 role 획득
        String email = jwtUtil.getUsername(token);
        Long memberId = jwtUtil.getMemberId(token);

        //user를 생성하여 값 set
        Member member = Member.builder()
                .email(email)
                .id(memberId)
                .build();

        //UserDetails에 회원 정보 객체 담기
        CustomSecurityUserDetails customSecurityUserDetails = new CustomSecurityUserDetails(member);

        //스프링 시큐리티 인증 토큰 생성
        Authentication authToken = new UsernamePasswordAuthenticationToken(customSecurityUserDetails, null, customSecurityUserDetails.getAuthorities());
        //세션에 사용자 등록
        SecurityContextHolder.getContext().setAuthentication(authToken);

        filterChain.doFilter(request, response);
    }
}
